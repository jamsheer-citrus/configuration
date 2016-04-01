package com.netspective.precis.resource.rest.templates;

import java.io.FileInputStream;
import java.io.InputStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.HashMap;
import java.util.Map;

import javax.servlet.http.HttpServletResponse;

import org.apache.log4j.Logger;
import org.apache.tomcat.util.http.fileupload.IOUtils;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;

import com.netspective.precis.common.ReadConfiguration;
import com.netspective.precis.common.Utils;
import com.netspective.precis.exception.ProtocolNestableException;
import com.netspective.precis.resource.persistence.EntityManager;
import com.netspective.precis.resource.rest.common.PrepareResponseMessage;
import com.netspective.precis.resource.rest.common.Validator;
import com.netspective.precis.resource.utils.Constants;
import com.netspective.precis.resource.utils.PrecisResourceURI;

@RestController
@RequestMapping("/api")
public class TemplateController {

	@Autowired
	private RestTemplate templateRestTemplate;

	static Logger logger = Logger.getLogger(TemplateController.class);
	StringWriter sw = new StringWriter();

	@RequestMapping(value = PrecisResourceURI.URI_TEMPLATES, method = RequestMethod.POST)
	Map<String, Object> addTemplates(@PathVariable String accountId,
			@RequestParam(value = "data", required = false) String body,
			@RequestParam(value = "file", required = false) MultipartFile files,
			@RequestHeader(value = Constants.OPERATED_BY, required = false) String operatedBy,
			@RequestHeader(value = Constants.ORGANIZATION_ID, required = false) String organizationId) {

		Map<String, Object> finalResponse = new HashMap<String, Object>();
		JSONObject responseMessage = new JSONObject();
		JSONObject jsonBody = null;
		String responseMsg = "";
		String notificationStyleId = "";
		Validator validator = new Validator();
		String eventTypeId = "";
		String notificationStyleSettingsId = "";
		if (organizationId == null) {
			organizationId = accountId;
		}
		try {
			if (operatedBy == null) {
				responseMessage.put(Constants.RESPONSE, ProtocolNestableException.MSG_OPERATED_BY_NOT_FOUND_ERROR);
				responseMsg = PrepareResponseMessage.PrepareErrorResponse(
						PrecisResourceURI.URI_TEMPLATES.replace("{accountId}", accountId), responseMessage);
				finalResponse = Utils.ConvertjsonStringToMap(responseMsg);
				logger.info(ProtocolNestableException.MSG_OPERATED_BY_NOT_FOUND_ERROR);
				return finalResponse;
			}
			if (files == null) {
				responseMessage.put(Constants.RESPONSE, ProtocolNestableException.MSG_TEMPLATE_FILE_NOT_FOUND);
				responseMsg = PrepareResponseMessage.PrepareErrorResponse(
						PrecisResourceURI.URI_TEMPLATES.replace("{accountId}", accountId), responseMessage);
				finalResponse = Utils.ConvertjsonStringToMap(responseMsg);
				logger.info(ProtocolNestableException.MSG_TEMPLATE_FILE_NOT_FOUND);
				return finalResponse;
			}
			if (body == null) {
				responseMessage.put(Constants.RESPONSE, ProtocolNestableException.MSG_BODY_NOT_FOUND);
				responseMsg = PrepareResponseMessage.PrepareErrorResponse(
						PrecisResourceURI.URI_TEMPLATES.replace("{accountId}", accountId), responseMessage);
				finalResponse = Utils.ConvertjsonStringToMap(responseMsg);
				logger.info(ProtocolNestableException.MSG_BODY_NOT_FOUND);
				return finalResponse;
			}
			jsonBody = new JSONObject(body);
			notificationStyleId = jsonBody.getString("notificationStyleId");
			eventTypeId = jsonBody.getString("eventTypeId");
			notificationStyleSettingsId = jsonBody.getString("notificationStyleSettingsId");
			if (!validator.validate(notificationStyleId, null, Constants.NOTIFICATION_STYLE_ID_CONST, accountId,
					organizationId, operatedBy, templateRestTemplate)) {
				responseMessage.put(Constants.RESPONSE, ProtocolNestableException.MSG_NOTIFICATION_ID_NOT_FOUND);
				responseMsg = PrepareResponseMessage.PrepareErrorResponse(
						PrecisResourceURI.URI_TEMPLATES.replace("{accountId}", accountId), responseMessage);
				finalResponse = Utils.ConvertjsonStringToMap(responseMsg);
				logger.info(ProtocolNestableException.MSG_NOTIFICATION_ID_NOT_FOUND);
				return finalResponse;
			}
			if (!validator.validate(eventTypeId, null, Constants.EVENT_TYPES_ID_CONST, accountId, organizationId,
					operatedBy, templateRestTemplate)) {
				responseMessage.put(Constants.RESPONSE, ProtocolNestableException.MSG_EVENT_TYPES_ID_NOT_FOUND);
				responseMsg = PrepareResponseMessage.PrepareErrorResponse(
						PrecisResourceURI.URI_TEMPLATES.replace("{accountId}", accountId), responseMessage);
				finalResponse = Utils.ConvertjsonStringToMap(responseMsg);
				logger.info(ProtocolNestableException.MSG_EVENT_TYPES_ID_NOT_FOUND);
				return finalResponse;
			}
			if (!validator.validate(notificationStyleSettingsId, null, Constants.APP_SETTINGS_ID_CONST, accountId,
					organizationId, operatedBy, templateRestTemplate)) {
				responseMessage.put(Constants.RESPONSE,
						ProtocolNestableException.MSG_NOTIFICATION_STYLE_SETTINGS_ID_NOT_FOUND);
				responseMsg = PrepareResponseMessage.PrepareErrorResponse(
						PrecisResourceURI.URI_TEMPLATES.replace("{accountId}", accountId), responseMessage);
				finalResponse = Utils.ConvertjsonStringToMap(responseMsg);
				logger.info(ProtocolNestableException.MSG_NOTIFICATION_STYLE_SETTINGS_ID_NOT_FOUND);
				return finalResponse;
			}
		} catch (JSONException e) {
			// TODO Auto-generated catch block
			e.printStackTrace(new PrintWriter(sw));
			logger.error(sw.toString());
			try {
				responseMessage.put(Constants.RESPONSE, e.getMessage());
			} catch (JSONException e1) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			responseMsg = PrepareResponseMessage.PrepareErrorResponse(
					PrecisResourceURI.URI_TEMPLATES.replace("{accountId}", accountId), responseMessage);
			finalResponse = Utils.ConvertjsonStringToMap(responseMsg);
			return finalResponse;
		}
		final String response = EntityManager.getInstance().saveEntity(accountId, organizationId, operatedBy, body,
				Constants.TEMPLATES_CONST, templateRestTemplate,
				PrecisResourceURI.URI_TEMPLATES.replace("{accountId}", accountId), files);
		finalResponse = Utils.ConvertjsonStringToMap(response);
		return finalResponse;
	}

	@RequestMapping(value = PrecisResourceURI.URI_TEMPLATES, method = RequestMethod.GET)
	Map<String, Object> getTemplate(@PathVariable String accountId,
			@RequestParam(value = Constants.PAGE_INDEX, required = false, defaultValue = Constants.PAGINATION_DEFAULT_START_INDEX) int pageIndex,
			@RequestParam(value = Constants.COUNT, required = false, defaultValue = Constants.PAGINATION_DEFAULT_COUNT) int count,
			@RequestHeader(value = Constants.OPERATED_BY, required = false) String operatedBy,
			@RequestHeader(value = Constants.ORGANIZATION_ID, required = false) String organizationId) {

		Map<String, Object> finalResponse = new HashMap<String, Object>();
		JSONObject responseMessage = new JSONObject();
		String responseFail = "";
		try {
			if (organizationId == null) {
				organizationId = accountId;
			}
			if (operatedBy == null) {

				responseMessage.put(Constants.RESPONSE, ProtocolNestableException.MSG_OPERATED_BY_NOT_FOUND_ERROR);
				responseFail = PrepareResponseMessage.PrepareErrorResponse(
						PrecisResourceURI.URI_TEMPLATES.replace("{accountId}", accountId), responseMessage);
				finalResponse = Utils.ConvertjsonStringToMap(responseFail);
				logger.info(ProtocolNestableException.MSG_OPERATED_BY_NOT_FOUND_ERROR);
				return finalResponse;
			}
			final String response = EntityManager.getInstance().getEntity(accountId, organizationId, operatedBy,
					pageIndex, count, Constants.TEMPLATES_CONST, templateRestTemplate,
					PrecisResourceURI.URI_TEMPLATES.replace("{accountId}", accountId));
			finalResponse = Utils.ConvertjsonStringToMap(response);
		} catch (Exception e) {
			e.printStackTrace(new PrintWriter(sw));
			logger.error(sw.toString());
		}
		return finalResponse;
	}

	@RequestMapping(value = PrecisResourceURI.URI_TEMPLATES_INFO_RESOURCES, method = RequestMethod.GET)
	Map<String, Object> getTemplateInfoById(@PathVariable String accountId, @PathVariable String templateId,
			@RequestHeader(value = Constants.OPERATED_BY, required = false) String operatedBy,
			@RequestHeader(value = Constants.ORGANIZATION_ID, required = false) String organizationId) {

		Map<String, Object> finalResponse = new HashMap<String, Object>();
		JSONObject responseMessage = new JSONObject();
		String responseSuccess = "";
		String URLResource = "";
		try {
			if (organizationId == null) {
				organizationId = accountId;
			}
			URLResource = PrecisResourceURI.URI_TEMPLATES_INFO_RESOURCES;
			URLResource = URLResource.replace("{accountId}", accountId);
			URLResource = URLResource.replace("{templateId}", templateId);
			if (operatedBy == null) {
				responseMessage.put(Constants.RESPONSE, ProtocolNestableException.MSG_OPERATED_BY_NOT_FOUND_ERROR);
				responseSuccess = PrepareResponseMessage.PrepareErrorResponse(URLResource, responseMessage);
				finalResponse = Utils.ConvertjsonStringToMap(responseSuccess);
				logger.info(ProtocolNestableException.MSG_OPERATED_BY_NOT_FOUND_ERROR);
				return finalResponse;
			}
			final String response = EntityManager.getInstance().getEntityById(accountId, templateId, organizationId,
					operatedBy, Constants.TEMPLATES_CONST, templateRestTemplate, URLResource);
			finalResponse = Utils.ConvertjsonStringToMap(response.toString());

		} catch (Exception e) {
			e.printStackTrace(new PrintWriter(sw));
			logger.error(sw.toString());
			try {
				responseMessage.put(Constants.RESPONSE, e.getMessage());
			} catch (JSONException e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			}
			responseSuccess = PrepareResponseMessage.PrepareErrorResponse(URLResource, responseMessage);
			finalResponse = Utils.ConvertjsonStringToMap(responseSuccess);
		}
		return finalResponse;
	}

	@RequestMapping(value = PrecisResourceURI.URI_TEMPLATES_RESOURCES, method = RequestMethod.GET)
	void getTemplateById(@PathVariable String accountId, @PathVariable String templateId, HttpServletResponse response,
			@RequestHeader(value = Constants.OPERATED_BY, required = false) String operatedBy,
			@RequestHeader(value = Constants.ORGANIZATION_ID, required = false) String organizationId) {

		ReadConfiguration readConfiguration = new ReadConfiguration();
		InputStream stream = null;
		String fileName = null;
		try {
			if (organizationId == null) {
				organizationId = accountId;
			}
			String URLResource = PrecisResourceURI.URI_TEMPLATES_RESOURCES;
			URLResource = URLResource.replace("{accountId}", accountId);
			URLResource = URLResource.replace("{templateId}", templateId);
			if (operatedBy == null) {
				response.setHeader(Constants.RESPONSE, ProtocolNestableException.MSG_OPERATED_BY_NOT_FOUND_ERROR);
				logger.info(ProtocolNestableException.MSG_OPERATED_BY_NOT_FOUND_ERROR);
				return;
			}
			fileName = EntityManager.getInstance().getEntityById(accountId, templateId, organizationId, operatedBy,
					Constants.TEMPLATES_CONST, templateRestTemplate, URLResource);
			stream = new FileInputStream(readConfiguration.getTemplateLocalPath() + "/" + fileName);
			IOUtils.copy(stream, response.getOutputStream());
			response.flushBuffer();
		} catch (Exception e) {
			e.printStackTrace(new PrintWriter(sw));
			logger.error(sw.toString());
			response.setHeader(Constants.RESPONSE, "Failed to get Templates due to backend error");
		}
	}

	@RequestMapping(value = PrecisResourceURI.URI_TEMPLATES_RESOURCES, method = RequestMethod.POST)
	Map<String, Object> updateTemplateById(@PathVariable String accountId, @PathVariable String templateId,
			@RequestParam(value = "file", required = false) MultipartFile files,
			@RequestParam(value = "body", required = false) String body,
			@RequestHeader(value = Constants.OPERATED_BY, required = false) String operatedBy,
			@RequestHeader(value = Constants.ORGANIZATION_ID, required = false) String organizationId) {
		Map<String, Object> finalResponse = new HashMap<String, Object>();
		JSONObject responseMessage = new JSONObject();
		JSONObject jsonBody = null;
		String responseMsg = "";
		String notificationStyleId = "";
		Validator validator = new Validator();
		String eventTypeId = "";
		String notificationStyleSettingsId = "";
		String URI = "";
		try {
			if (organizationId == null) {
				organizationId = accountId;
			}
			URI = PrecisResourceURI.URI_TEMPLATES_RESOURCES;
			URI = URI.replace("{accountId}", accountId);
			URI = URI.replace("{templateId}", templateId);
			if (operatedBy == null) {
				responseMessage.put(Constants.RESPONSE, ProtocolNestableException.MSG_OPERATED_BY_NOT_FOUND_ERROR);
				String responseFail = PrepareResponseMessage.PrepareErrorResponse(URI, responseMessage);
				finalResponse = Utils.ConvertjsonStringToMap(responseFail);
				logger.info(ProtocolNestableException.MSG_OPERATED_BY_NOT_FOUND_ERROR);
				return finalResponse;
			}
			if (files == null) {
				responseMessage.put(Constants.RESPONSE, ProtocolNestableException.MSG_TEMPLATE_FILE_NOT_FOUND);
				responseMsg = PrepareResponseMessage.PrepareErrorResponse(URI, responseMessage);
				logger.info(ProtocolNestableException.MSG_TEMPLATE_FILE_NOT_FOUND);
				finalResponse = Utils.ConvertjsonStringToMap(responseMsg);
				return finalResponse;
			}
			if (body == null) {
				responseMessage.put(Constants.RESPONSE, ProtocolNestableException.MSG_BODY_NOT_FOUND);
				responseMsg = PrepareResponseMessage.PrepareErrorResponse(URI, responseMessage);
				finalResponse = Utils.ConvertjsonStringToMap(responseMsg);
				logger.info(ProtocolNestableException.MSG_BODY_NOT_FOUND);
				return finalResponse;
			}
			jsonBody = new JSONObject(body);
			notificationStyleId = jsonBody.getString("notificationStyleId");
			eventTypeId = jsonBody.getString("eventTypeId");
			notificationStyleSettingsId = jsonBody.getString("notificationStyleSettingsId");
			if (!validator.validate(notificationStyleId, null, Constants.NOTIFICATION_STYLE_ID_CONST, accountId,
					organizationId, operatedBy, templateRestTemplate)) {
				responseMessage.put(Constants.RESPONSE, ProtocolNestableException.MSG_NOTIFICATION_ID_NOT_FOUND);
				responseMsg = PrepareResponseMessage.PrepareErrorResponse(URI, responseMessage);
				finalResponse = Utils.ConvertjsonStringToMap(responseMsg);
				logger.info(ProtocolNestableException.MSG_NOTIFICATION_ID_NOT_FOUND);
				return finalResponse;
			}
			if (!validator.validate(eventTypeId, null, Constants.EVENT_TYPES_ID_CONST, accountId, organizationId,
					operatedBy, templateRestTemplate)) {
				responseMessage.put(Constants.RESPONSE, ProtocolNestableException.MSG_EVENT_TYPES_ID_NOT_FOUND);
				responseMsg = PrepareResponseMessage.PrepareErrorResponse(URI, responseMessage);
				finalResponse = Utils.ConvertjsonStringToMap(responseMsg);
				logger.info(ProtocolNestableException.MSG_EVENT_TYPES_ID_NOT_FOUND);
				return finalResponse;
			}
			if (!validator.validate(notificationStyleSettingsId, null, Constants.APP_SETTINGS_ID_CONST, accountId,
					organizationId, operatedBy, templateRestTemplate)) {
				responseMessage.put(Constants.RESPONSE,
						ProtocolNestableException.MSG_NOTIFICATION_STYLE_SETTINGS_ID_NOT_FOUND);
				responseMsg = PrepareResponseMessage.PrepareErrorResponse(URI, responseMessage);
				finalResponse = Utils.ConvertjsonStringToMap(responseMsg);
				logger.info(ProtocolNestableException.MSG_NOTIFICATION_STYLE_SETTINGS_ID_NOT_FOUND);
				return finalResponse;
			}
			final String response = EntityManager.getInstance().updateEntity(accountId, templateId, organizationId,
					operatedBy, body, Constants.TEMPLATES_CONST, templateRestTemplate, URI, files);

			finalResponse = Utils.ConvertjsonStringToMap(response.toString());
		} catch (Exception e) {
			e.printStackTrace(new PrintWriter(sw));
			logger.error(sw.toString());
			try {
				responseMessage.put(Constants.RESPONSE, e.getMessage());
			} catch (JSONException e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			}
			responseMsg = PrepareResponseMessage.PrepareErrorResponse(URI, responseMessage);
			finalResponse = Utils.ConvertjsonStringToMap(responseMsg);
		}
		return finalResponse;
	}

	@RequestMapping(value = PrecisResourceURI.URI_TEMPLATES_RESOURCES, method = RequestMethod.DELETE)
	Map<String, Object> deleteTemplateById(@PathVariable String accountId, @PathVariable String templateId,
			@RequestHeader(value = Constants.OPERATED_BY, required = false) String operatedBy,
			@RequestHeader(value = Constants.ORGANIZATION_ID, required = false) String organizationId) {
		Map<String, Object> finalResponse = new HashMap<String, Object>();
		JSONObject responseMessage = new JSONObject();
		String responseFail = "";
		String URLResource = "";
		String responseSuccess = "";
		try {
			if (organizationId == null) {
				organizationId = accountId;
			}
			URLResource = PrecisResourceURI.URI_TEMPLATES_RESOURCES;
			URLResource = URLResource.replace("{accountId}", accountId);
			URLResource = URLResource.replace("{templateId}", templateId);
			if (operatedBy == null) {
				try {
					responseMessage.put(Constants.RESPONSE, ProtocolNestableException.MSG_OPERATED_BY_NOT_FOUND_ERROR);
				} catch (JSONException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				responseSuccess = PrepareResponseMessage.PrepareErrorResponse(URLResource, responseMessage);
				finalResponse = Utils.ConvertjsonStringToMap(responseSuccess);
				logger.info(ProtocolNestableException.MSG_OPERATED_BY_NOT_FOUND_ERROR);
				return finalResponse;
			}
			final String response = EntityManager.getInstance().deleteEntity(accountId, templateId, organizationId,
					operatedBy, Constants.TEMPLATES_CONST, templateRestTemplate, URLResource);
			finalResponse = Utils.ConvertjsonStringToMap(response.toString());
		} catch (Exception e) {
			e.printStackTrace(new PrintWriter(sw));
			logger.error(sw.toString());
		}
		return finalResponse;
	}

}
