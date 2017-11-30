package it.csi.bigiot.parkingingest.service;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import it.csi.bigiot.parkingingest.model.ParkLot;

@Component
public class NedapReader {
	private static final Logger log = LoggerFactory.getLogger(NedapReader.class);

	@Autowired
	public JavaMailSender emailSender;

	// set this to false to disable this job; set it it true by
	@Value("${ingester.enabled:false}")
	private boolean scheduledJobEnabled;

	@Value("${SDP_auth}")
	private String SDP_auth;

	HashMap<String, ParkLot> previousRead = new HashMap<String, ParkLot>();
	// JSONParser parser = new JSONParser();
	@Value("#{${parkSDPLots}}")
	private Map<String, String> parkSDPLots;

	private RestTemplate restTemplate = new RestTemplate();

	@Scheduled(fixedRateString = "${ingester.fixedRate}") // every 30 seconds
	public void readParkingData() {
		log.info("scheduled read");
		if (!scheduledJobEnabled) {
			return;
		}

		log.info("LOT INFO");
		log.info(parkSDPLots.toString());
		log.info(parkSDPLots.get("LOT_011"));
		String transactionUrl = "http://csipiemonte.tmacs.it:8080/csipiemontewebservices/webresources/park/getparkstate";

		UriComponentsBuilder builder = UriComponentsBuilder.fromUriString(transactionUrl)
				// Add query parameter
				.queryParam("park_code", "PARK_01_VIALE_VIOTTI");

		try {
			String response = restTemplate.getForObject(builder.toUriString(), String.class);
			log.info(response);
			JSONObject jresponse = new JSONObject(response);
			log.info(jresponse.getString("validity"));
			if ((jresponse != null) && (jresponse.getString("validity").equals("good"))) {
				log.info(jresponse.get("lots").toString());
				JSONArray lots = jresponse.getJSONArray("lots");

				for (int i = 0; i < lots.length(); i++) {
					JSONObject aLot = lots.getJSONObject(i);
					if (previousRead.containsKey(aLot.getString("lot_code"))) {
						log.info("Already red the lot: " + aLot.getString("lot_code"));
						if (!((ParkLot) previousRead.get(aLot.getString("lot_code"))).getStatus()
								.equals(aLot.getString("lot_state"))) {
							publishToSDP(aLot);
							ParkLot changedLot = new ParkLot(aLot.getString("lot_state"), aLot.getLong("lot_timestamp"));
							previousRead.put(aLot.getString("lot_code"), changedLot);
						}
					} else {
						ParkLot newLot = new ParkLot(aLot.getString("lot_state"), aLot.getLong("lot_timestamp"));
						previousRead.put(aLot.getString("lot_code"), newLot);
					}
				}

			}else{
				log.info("DATA FROM CLOUD NOT VALID!!!!!!");
			}
		} catch (Exception e) {
			// TODO Auto-generated catch block
			log.info("eccezione gestita");
			e.printStackTrace();

			SimpleMailMessage message = new SimpleMailMessage();
			message.setTo("luca.gioppo@csi.it");
			message.setSubject("********** Error from NedapReader *********");
			message.setText(e.toString());
			emailSender.send(message);

		}

	}

	/**
	 * @param aLot
	 * @throws JSONException
	 */
	private void publishToSDP(JSONObject aLot) throws JSONException {
		// Now check with the previous data if the data is the
		// same or not

		// changed status
		log.info("changed status from: " + ((ParkLot) previousRead.get(aLot.getString("lot_code"))).getStatus() + " to "
				+ aLot.getString("lot_state"));

		Date changeTime = new Date((long) aLot.getLong("lot_timestamp") * 1000);
		DateFormat f = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'");
		log.info(f.format(changeTime));

		String parkSDPUrl = "https://stream.smartdatanet.it/api/input/vercelli_bigiot";
		log.info(parkSDPLots.get(aLot.getString("lot_code")));
		log.info("changing lot is: " + aLot.getString("lot_code"));
		log.info(parkSDPLots.toString());

		HttpHeaders headers = new HttpHeaders();
		headers.add("Content-Type", "application/json");
		headers.add("Authorization", "Basic " + SDP_auth);
		String sdpPostData = "{\"sensor\": \"" + parkSDPLots.get(aLot.getString("lot_code"))
				+ "\", \"stream\": \"parkingStatus\", \"values\": [{ \"time\": \"" + f.format(changeTime)
				+ "\", \"components\": {\"status\": \"" + aLot.getString("lot_state") + "\"}}]}";

		log.info(sdpPostData);
		HttpEntity<String> entity = new HttpEntity<String>(sdpPostData, headers);

		ResponseEntity<String> sdpResponse = restTemplate.exchange(parkSDPUrl, HttpMethod.POST, entity, String.class);
		if (sdpResponse.getStatusCode() == HttpStatus.OK) {
			JSONObject userJson = new JSONObject(sdpResponse.getBody());
			log.info(sdpResponse.toString());
		} else if (sdpResponse.getStatusCode() == HttpStatus.UNAUTHORIZED) {
			// nono... bad credentials
			log.info(sdpResponse.toString());
		} else {
			log.info(sdpResponse.toString());
		}

	}

}

/*
 * headers) Authorization = Basic YmllbGxhX2JpZ2lvdDplRThjYkxGTyQ3QzE=
 * Content-Type = application/json
 * 
 * body) Cambiando "sensor" nel json qua sotto, cambi lo stream verso cui
 * spedisci i dati
 * 
 * ds_Datiaria_2255 <- AQSBI01 806d2a6f-0fdb-466b-f522-8743ae520a15
 * ds_Datiaria_2265 <- AQSBI02 7ec4a7e5-f9fe-4826-befa-3e05e247ef32
 * ds_Datiaria_2266 <- AQSBI03 fcc8e834-6c0b-45a4-d6a5-03b32dd69fa2
 * ds_Datiaria_2267 <- AQSBI04 e3f97f46-abc3-4a95-a878-a2e5df52aa1e
 * 
 * 
 * {"sensor": "806d2a6f-0fdb-466b-f522-8743ae520a15", "stream": "datiaria",
 * "values": [{ "time": "2017-09-13T12:39:40Z", "components": { "status_co2":
 * "OK", "status_co": "OK", "status_o3": "OK", "status_no2": "OK",
 * "status_pm10": "OK", "status_pm25": "OK", "status_t": "OK", "status_h": "OK",
 * "co2": 410.0, "co": 1.5, "o3": 1.5, "no2": 1.5, "pm10": 720.0, "pm25": 1.5,
 * "t": 28.5, "h": 32.4} }] }
 */
