package org.ekstep.language.controller;

import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.ekstep.language.common.enums.LanguageActorNames;
import org.ekstep.language.common.enums.LanguageErrorCodes;
import org.ekstep.language.common.enums.LanguageOperations;
import org.ekstep.language.common.enums.LanguageParams;
import org.ekstep.language.mgr.IParserManager;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;

import com.ilimi.common.dto.Request;
import com.ilimi.common.dto.Response;
import com.ilimi.common.exception.ClientException;
import com.ilimi.common.util.ILogger;
import com.ilimi.common.util.PlatformLogger;
import com.ilimi.common.util.PlatformLogger;;
import com.ilimi.common.util.PlatformLogManager;
import com.ilimi.common.util.PlatformLogger;

@Controller
@RequestMapping("v3/tools")
public class ToolsV3Controller extends BaseLanguageController {

	/** The logger. */
	private static ILogger LOGGER = PlatformLogManager.getLogger();

	/** The parser manger. */
	@Autowired
	private IParserManager parserManger;

	/**
	 * Parses the content.
	 *
	 * @param map
	 *            the map
	 * @param userId
	 *            the user id
	 * @return the response entity
	 */
	@RequestMapping(value = "/parser", method = RequestMethod.POST)
	@ResponseBody
	public ResponseEntity<Response> parseContent(@RequestBody Map<String, Object> map,
			@RequestHeader(value = "user-id") String userId) {
		String apiId = "ekstep.language.parser";
		Request request = getRequest(map);
		try {
			String languageId = (String) request.get(LanguageParams.language_id.name());
			if (StringUtils.isBlank(languageId))
				throw new ClientException(LanguageErrorCodes.ERR_INVALID_LANGUAGE_ID.name(), "Invalid language id");
			String content = (String) request.get(LanguageParams.content.name());
			if (StringUtils.isBlank(content))
				throw new ClientException(LanguageErrorCodes.ERR_INVALID_CONTENT.name(), "Cannot parse empty content");
			Boolean wordSuggestions = (Boolean) request.get("wordSuggestions");
			// Boolean relatedWords = (Boolean) request.get("relatedWords");
			Boolean translations = (Boolean) request.get("translations");
			Boolean equivalentWords = (Boolean) request.get("equivalentWords");
			Integer limit = (Integer) request.get("limit");
			Response response = parserManger.parseContent(languageId, content, wordSuggestions, false, translations,
					equivalentWords, limit);
			LOGGER.log("Parser | Response: " + response);
			return getResponseEntity(response, apiId,
					(null != request.getParams()) ? request.getParams().getMsgid() : null);
		} catch (Exception e) {
			LOGGER.log("Parser | Exception: " , e.getMessage(), e);
			return getExceptionResponseEntity(e, apiId,
					(null != request.getParams()) ? request.getParams().getMsgid() : null);
		}
	}

	/**
	 * Computes and returns the complexity measures of a word or a text.
	 *
	 * @param map
	 *            the map
	 * @return the complexity
	 */
	@RequestMapping(value = "/complexity", method = RequestMethod.POST)
	@ResponseBody
	public ResponseEntity<Response> getComplexity(@RequestBody Map<String, Object> map) {
		String apiId = "ekstep.language.complexity.info";
		Request request = getRequest(map);
		String language = (String) request.get(LanguageParams.language_id.name());
		// TODO: return error response if language value is blank
		request.setManagerName(LanguageActorNames.LEXILE_MEASURES_ACTOR.name());
		request.setOperation(LanguageOperations.computeComplexity.name());
		request.getContext().put(LanguageParams.language_id.name(), language);
		LOGGER.log("List | Request: " + request);
		try {
			Response response = getResponse(request, LOGGER);
			LOGGER.log("List | Response: " + response);
			return getResponseEntity(response, apiId,
					(null != request.getParams()) ? request.getParams().getMsgid() : null);
		} catch (Exception e) {
			LOGGER.log("List | Exception: " , e.getMessage(), e);
			return getExceptionResponseEntity(e, apiId,
					(null != request.getParams()) ? request.getParams().getMsgid() : null);
		}
	}
	
	/**
	 * Computes and returns the complexity measures of a text.
	 *
	 * @param map
	 *            the map
	 * @return the response entity
	 */
	@RequestMapping(value = "/text/analysis", method = RequestMethod.POST)
	@ResponseBody
	public ResponseEntity<Response> computeTextComplexity(@RequestBody Map<String, Object> map) {
		String apiId = "ekstep.language.text.complexity.info";
		Request request = getRequest(map);
		String language = (String) request.get(LanguageParams.language_id.name());
		request.setManagerName(LanguageActorNames.LEXILE_MEASURES_ACTOR.name());
		request.setOperation(LanguageOperations.computeTextComplexity.name());
		request.getContext().put(LanguageParams.language_id.name(), language);
		LOGGER.log("List | Request: " + request);
		try {
			Response response = getResponse(request, LOGGER);
			LOGGER.log("List | Response: " + response);
			return getResponseEntity(response, apiId,
					(null != request.getParams()) ? request.getParams().getMsgid() : null);
		} catch (Exception e) {
			LOGGER.log("List | Exception: " , e.getMessage(), e);
			return getExceptionResponseEntity(e, apiId,
					(null != request.getParams()) ? request.getParams().getMsgid() : null);
		}
	}
}
