package com.ilimi.taxonomy.controller;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletResponse;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.multipart.MultipartFile;

import com.ilimi.common.controller.BaseController;
import com.ilimi.common.dto.Request;
import com.ilimi.common.dto.Response;
import com.ilimi.common.logger.LogHelper;
import com.ilimi.common.util.ILogger;
import com.ilimi.common.util.PlatformLogger;
import com.ilimi.common.util.PlatformLogger;;
import com.ilimi.common.util.PlatformLogManager;
import com.ilimi.common.util.PlatformLogger;
import com.ilimi.graph.common.enums.GraphEngineParams;
import com.ilimi.graph.dac.model.SearchCriteria;
import com.ilimi.graph.enums.ImportType;
import com.ilimi.graph.importer.OutputStreamValue;
import com.ilimi.taxonomy.enums.TaxonomyAPIParams;
import com.ilimi.taxonomy.mgr.ITaxonomyManager;

@Controller
@RequestMapping("/taxonomy")
public class TaxonomyController extends BaseController {

	private static ILogger LOGGER = PlatformLogManager.getLogger();

	@Autowired
	private ITaxonomyManager taxonomyManager;

	@RequestMapping(value = "/{graphId:.+}/{objectType:.+}", method = RequestMethod.GET)
	@ResponseBody
	public ResponseEntity<Response> findAllByObjectType(@PathVariable(value = "graphId") String graphId,
			@PathVariable(value = "objectType") String objectType, @RequestHeader(value = "user-id") String userId) {
		String apiId = "ekstep.taxonomy.objecttype.list";
		try {
			Response response = taxonomyManager.findAllByObjectType(graphId, objectType);
			LOGGER.log("FindAll | Response: " , response);
			return getResponseEntity(response, apiId, null);
		} catch (Exception e) {
			LOGGER.log("FindAll | Exception: " , e.getMessage(), e);
			return getExceptionResponseEntity(e, apiId, null);
		}
	}

	@RequestMapping(value = "/{id:.+}", method = RequestMethod.POST)
	@ResponseBody
	public ResponseEntity<Response> create(@PathVariable(value = "id") String id,
			@RequestParam("file") MultipartFile file, @RequestHeader(value = "user-id") String userId,
			HttpServletResponse resp) {
		String apiId = "ekstep.taxonomy.import";
		LOGGER.log("Create | Id: " + id + " | File: " + file + " | user-id: " + userId);
		InputStream stream = null;
		try {
			if (null != file)
				stream = file.getInputStream();
			Response response = taxonomyManager.create(id, stream);
			LOGGER.log("Create | Response: " , response);
			return getResponseEntity(response, apiId, null);
		} catch (Exception e) {
			LOGGER.log("Create | Exception: " , e.getMessage(), e);
			return getExceptionResponseEntity(e, apiId, null);
		} finally {
			if (null != stream)
				try {
					stream.close();
				} catch (IOException e) {
					LOGGER.log("Error1 While Closing the Stream.", e.getMessage(), e);
				}
		}
	}

	@RequestMapping(value = "/{id:.+}/export", method = RequestMethod.POST)
	@ResponseBody
	public void export(@PathVariable(value = "id") String id, @RequestBody Map<String, Object> map,
			@RequestHeader(value = "user-id") String userId, HttpServletResponse resp) {
		String format = ImportType.CSV.name();
		String apiId = "ekstep.taxonomy.export";
		LOGGER.log("Export | Id: " + id + " | Format: " + format + " | user-id: " + userId);
		try {
			Request req = getRequest(map);
			try {
				SearchCriteria criteria = mapper.convertValue(req.get(TaxonomyAPIParams.search_criteria.name()),
						SearchCriteria.class);
				req.put(TaxonomyAPIParams.search_criteria.name(), criteria);
			} catch (Exception e) {
			}
			req.put(GraphEngineParams.format.name(), format);
			Response response = taxonomyManager.export(id, req);
			if (!checkError(response)) {
				OutputStreamValue graphOutputStream = (OutputStreamValue) response
						.get(GraphEngineParams.output_stream.name());
				try (OutputStream os = graphOutputStream.getOutputStream();
						ByteArrayOutputStream bos = (ByteArrayOutputStream) os) {
					byte[] bytes = bos.toByteArray();
					resp.setContentType("text/csv");
					resp.setHeader("Content-Disposition", "attachment; filename=graph.csv");
					resp.getOutputStream().write(bytes);
					resp.getOutputStream().close();
				}
			}
			LOGGER.log("Export | Response: " , response);
		} catch (Exception e) {
			LOGGER.log("Create | Exception: " , e.getMessage(), e);
		}
	}

	@RequestMapping(value = "/{id:.+}", method = RequestMethod.DELETE)
	@ResponseBody
	public ResponseEntity<Response> delete(@PathVariable(value = "id") String id,
			@RequestHeader(value = "user-id") String userId) {
		LOGGER.log("Delete | Id: " + id + " | user-id: " + userId);
		String apiId = "ekstep.taxonomy.delete";
		try {
			Response response = taxonomyManager.delete(id);
			LOGGER.log("Delete | Response: " , response);
			return getResponseEntity(response, apiId, null);
		} catch (Exception e) {
			LOGGER.log("Delete | Exception: " , e.getMessage(), e);
			e.printStackTrace();
			return getExceptionResponseEntity(e, apiId, null);
		}
	}

	@RequestMapping(value = "/{id:.+}/definition", method = RequestMethod.POST)
	@ResponseBody
	public ResponseEntity<Response> createDefinition(@PathVariable(value = "id") String id, @RequestBody String json,
			@RequestHeader(value = "user-id") String userId) {
		String apiId = "ekstep.definition.create";
		LOGGER.log("Create Definition | Id: " + id + " | user-id: " + userId);
		try {
			Response response = taxonomyManager.updateDefinition(id, json);
			LOGGER.log("Create Definition | Response: " , response);
			return getResponseEntity(response, apiId, null);
		} catch (Exception e) {
			LOGGER.log("Create Definition | Exception: " , e.getMessage(), e);
			return getExceptionResponseEntity(e, apiId, null);
		}
	}

	@RequestMapping(value = "/{id:.+}/definition/{defId:.+}", method = RequestMethod.GET)
	@ResponseBody
	public ResponseEntity<Response> findDefinition(@PathVariable(value = "id") String id,
			@PathVariable(value = "defId") String objectType, @RequestHeader(value = "user-id") String userId) {
		String apiId = "ekstep.definition.find";
		LOGGER.log("Find Definition | Id: " + id + " | Object Type: " + objectType + " | user-id: " + userId);
		try {
			Response response = taxonomyManager.findDefinition(id, objectType);
			LOGGER.log("Find Definition | Response: " , response);
			return getResponseEntity(response, apiId, null);
		} catch (Exception e) {
			LOGGER.log("Find Definition | Exception: " , e.getMessage(), e);
			return getExceptionResponseEntity(e, apiId, null);
		}
	}

	@RequestMapping(value = "/{id:.+}/definition", method = RequestMethod.GET)
	@ResponseBody
	public ResponseEntity<Response> findAllDefinitions(@PathVariable(value = "id") String id,
			@RequestHeader(value = "user-id") String userId) {
		String apiId = "ekstep.definition.list";
		LOGGER.log("Find All Definitions | Id: " + id + " | user-id: " + userId);
		try {
			Response response = taxonomyManager.findAllDefinitions(id);
			LOGGER.log("Find All Definitions | Response: " , response);
			return getResponseEntity(response, apiId, null);
		} catch (Exception e) {
			LOGGER.log("Find All Definitions | Exception: " , e.getMessage(), e);
			return getExceptionResponseEntity(e, apiId, null);
		}
	}

	@RequestMapping(value = "/{id:.+}/definition/{defId:.+}", method = RequestMethod.DELETE)
	@ResponseBody
	public ResponseEntity<Response> deleteDefinition(@PathVariable(value = "id") String id,
			@PathVariable(value = "defId") String objectType, @RequestHeader(value = "user-id") String userId) {
		String apiId = "ekstep.definition.delete";
		LOGGER.log("Delete Definition | Id: " + id + " | Object Type: " + objectType + " | user-id: " + userId);
		try {
			Response response = taxonomyManager.deleteDefinition(id, objectType);
			LOGGER.log("Delete Definition | Response: " + response);
			return getResponseEntity(response, apiId, null);
		} catch (Exception e) {
			LOGGER.log("Delete Definition | Exception: " + e.getMessage(), e);
			return getExceptionResponseEntity(e, apiId, null);
		}
	}

	@SuppressWarnings("unchecked")
	@RequestMapping(value = "/{id:.+}/index", method = RequestMethod.POST)
	@ResponseBody
	public ResponseEntity<Response> createIndex(@PathVariable(value = "id") String id,
			@RequestBody Map<String, Object> map, @RequestHeader(value = "user-id") String userId) {
		String apiId = "ekstep.index.create";
		Request request = getRequest(map);
		LOGGER.log("Create Index | Id: " + id + " | Request: " + request + " | user-id: " + userId);
		try {
			List<String> keys = (List<String>) request.get(TaxonomyAPIParams.property_keys.name());
			Boolean unique = (Boolean) request.get(TaxonomyAPIParams.unique_constraint.name());
			Response response = taxonomyManager.createIndex(id, keys, unique);
			LOGGER.log("Create Index | Response: " , response);
			return getResponseEntity(response, apiId,
					(null != request.getParams()) ? request.getParams().getMsgid() : null);
		} catch (Exception e) {
			LOGGER.log("Create Index | Exception: " , e.getMessage(), e);
			return getExceptionResponseEntity(e, apiId,
					(null != request.getParams()) ? request.getParams().getMsgid() : null);
		}
	}

}
