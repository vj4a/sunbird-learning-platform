package org.ekstep.learning.router;

import org.apache.commons.lang3.StringUtils;
import org.ekstep.learning.actor.ContentStoreActor;
import org.ekstep.learning.common.enums.LearningActorNames;
import org.ekstep.learning.common.enums.LearningErrorCodes;

import com.ilimi.common.dto.Request;
import com.ilimi.common.dto.Response;
import com.ilimi.common.dto.ResponseParams;
import com.ilimi.common.dto.ResponseParams.StatusType;
import com.ilimi.common.exception.ClientException;
import com.ilimi.common.exception.MiddlewareException;
import com.ilimi.common.exception.ResourceNotFoundException;
import com.ilimi.common.exception.ResponseCode;
import com.ilimi.common.exception.ServerException;
import com.ilimi.common.router.RequestRouterPool;
import com.ilimi.common.util.ILogger;
import com.ilimi.common.util.PlatformLogger;
import com.ilimi.common.util.PlatformLogger;;
import com.ilimi.common.util.PlatformLogManager;
import com.ilimi.common.util.PlatformLogger;

import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import akka.actor.Props;
import akka.actor.UntypedActor;
import akka.dispatch.OnFailure;
import akka.dispatch.OnSuccess;
import akka.pattern.Patterns;
import akka.routing.SmallestMailboxPool;
import scala.concurrent.Future;

// TODO: Auto-generated Javadoc
/**
 * The Class LearningRequestRouter, provides functionality to route the request
 * for all learning actors
 *
 * @author karthik
 */
public class LearningRequestRouter extends UntypedActor {

	/** The logger. */
	private static ILogger LOGGER = PlatformLogManager.getLogger();
	private static final String ekstep = "org.ekstep.";
	private static final String ilimi = "com.ilimi.";
	private static final String java = "java.";
	private static final String default_err_msg = "Something went wrong in server while processing the request";

	/** The timeout. */
	protected long timeout = 30000;

	/*
	 * (non-Javadoc)
	 * 
	 * @see akka.actor.UntypedActor#onReceive(java.lang.Object)
	 */
	@Override
	public void onReceive(Object message) throws Exception {
		if (message instanceof String) {
			if (StringUtils.equalsIgnoreCase("init", message.toString())) {
				initActorPool();
				getSender().tell("initComplete", getSelf());
			} else {
				getSender().tell(message, getSelf());
			}
		} else if (message instanceof Request) {
			Request request = (Request) message;
			ActorRef parent = getSender();
			try {
				ActorRef actorRef = getActorFromPool(request);
				long t = timeout;
				Future<Object> future = Patterns.ask(actorRef, request, t);
				handleFuture(request, future, parent);
			} catch (Exception e) {
				handleException(request, e, parent);
			}
		}
	}

	/**
	 * Inits the actor pool.
	 */
	private void initActorPool() {
		ActorSystem system = RequestRouterPool.getActorSystem();
		int poolSize = 4;

		Props contentStoreProps = Props.create(ContentStoreActor.class);
		ActorRef contentStoreActor = system.actorOf(new SmallestMailboxPool(poolSize).props(contentStoreProps));
		LearningActorPool.addActorRefToPool(LearningActorNames.CONTENT_STORE_ACTOR.name(), contentStoreActor);
	}

	/**
	 * Gets the actor from pool.
	 *
	 * @param request
	 *            the request
	 * @return the actor from pool
	 */
	private ActorRef getActorFromPool(Request request) {
		String manager = request.getManagerName();
		ActorRef ref = LearningActorPool.getActorRefFromPool(manager);
		if (null == ref)
			throw new ClientException(LearningErrorCodes.ERR_ROUTER_ACTOR_NOT_FOUND.name(),
					"Actor not found in the pool for manager: " + manager);
		return ref;
	}

	/**
	 * Handle future.
	 *
	 * @param request
	 *            the request
	 * @param future
	 *            the future
	 * @param parent
	 *            the parent
	 */
	protected void handleFuture(final Request request, Future<Object> future, final ActorRef parent) {
		future.onSuccess(new OnSuccess<Object>() {
			@Override
			public void onSuccess(Object arg0) throws Throwable {
				parent.tell(arg0, getSelf());
				Response res = (Response) arg0;
				ResponseParams params = res.getParams();
				LOGGER.log(
						request.getManagerName() , request.getOperation() + ", SUCCESS, " + params.toString());
			}
		}, getContext().dispatcher());

		future.onFailure(new OnFailure() {
			@Override
			public void onFailure(Throwable e) throws Throwable {
				handleException(request, e, parent);
			}
		}, getContext().dispatcher());
	}

	/**
	 * Handle exception.
	 *
	 * @param request
	 *            the request
	 * @param e
	 *            the e
	 * @param parent
	 *            the parent
	 */
	protected void handleException(final Request request, Throwable e, final ActorRef parent) {
		LOGGER.log(request.getManagerName() + "," + request.getOperation() , ", ERROR: " + e.getMessage(), "WARN");
		Response response = new Response();
		ResponseParams params = new ResponseParams();
		params.setStatus(StatusType.failed.name());
		if (e instanceof MiddlewareException) {
			MiddlewareException mwException = (MiddlewareException) e;
			params.setErr(mwException.getErrCode());
		} else {
			params.setErr(LearningErrorCodes.ERR_SYSTEM_EXCEPTION.name());
		}
		params.setErrmsg(setErrMessage(e));
		response.setParams(params);
		setResponseCode(response, e);
		parent.tell(response, getSelf());
	}

	private String setErrMessage(Throwable e) {
		Class<? extends Throwable> className = e.getClass();
		if (className.getName().contains(ekstep) || className.getName().contains(ilimi)) {
			LOGGER.log("Setting error message sent from class " + className + e.getMessage());
			return e.getMessage();
		} else if (className.getName().startsWith(java)) {
			LOGGER.log("Setting default err msg " + className + e.getMessage());
			return default_err_msg;
		}
		return "";
	}

	/**
	 * Sets the response code.
	 *
	 * @param res
	 *            the res
	 * @param e
	 *            the e
	 */
	private void setResponseCode(Response res, Throwable e) {
		if (e instanceof ClientException) {
			res.setResponseCode(ResponseCode.CLIENT_ERROR);
		} else if (e instanceof ServerException) {
			res.setResponseCode(ResponseCode.SERVER_ERROR);
		} else if (e instanceof ResourceNotFoundException) {
			res.setResponseCode(ResponseCode.RESOURCE_NOT_FOUND);
		} else {
			res.setResponseCode(ResponseCode.SERVER_ERROR);
		}
	}
}