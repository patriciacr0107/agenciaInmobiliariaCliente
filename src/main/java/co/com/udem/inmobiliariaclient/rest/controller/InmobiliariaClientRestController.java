package co.com.udem.inmobiliariaclient.rest.controller;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.gson.Gson;

import co.com.udem.inmobiliariaclient.domain.AutenticationRequestDTO;
import co.com.udem.inmobiliariaclient.domain.AutenticationResponseDTO;
import co.com.udem.inmobiliariaclient.domain.PropiedadDTO;
import co.com.udem.inmobiliariaclient.domain.UsuarioDTO;
import co.com.udem.inmobiliariaclient.entities.UserToken;
import co.com.udem.inmobiliariaclient.repositories.UserTokenRepository;
import co.com.udem.inmobiliariaclient.util.Constantes;

@RestController
public class InmobiliariaClientRestController {
	@Autowired
	RestTemplate restTemplate;

	@Autowired
	UserTokenRepository userTokenRepository;

	@Autowired
	UserToken userToken;

	@PostMapping("/autenticar")
	public Map<String, String> autenticar(@RequestBody AutenticationRequestDTO autenticationRequestDTO) {
		Map<String, String> response = new HashMap<>();
		try {

			ResponseEntity<String> postResponse = restTemplate.postForEntity("http://localhost:9090/auth/signin",
					autenticationRequestDTO, String.class);
			Gson g = new Gson();
			AutenticationResponseDTO autenticationResponseDTO = g.fromJson(postResponse.getBody(),
					AutenticationResponseDTO.class);

			if (userTokenRepository.obtenerEntidadToken(autenticationResponseDTO.getUsername()).isPresent()) {
				userToken = userTokenRepository.obtenerEntidadToken(autenticationResponseDTO.getUsername()).get();
			}

			userToken.setUsername(autenticationResponseDTO.getUsername());
			userToken.setToken(autenticationResponseDTO.getToken());
			userTokenRepository.save(userToken);
			response.put(Constantes.CODIGO_HTTP, "200");
			response.put(Constantes.MENSAJE_ERROR, autenticationResponseDTO.getToken());
			return response;
		} catch (Exception e) {
			response.put(Constantes.CODIGO_HTTP, "500");
			response.put(Constantes.MENSAJE_ERROR, "Usuario o contraseña invalido");
			return response;
		}
	}

	@GetMapping("/consultarPropiedades/{username}")
	public List<PropiedadDTO> consultarPropiedad(@PathVariable String username) {
		List<PropiedadDTO> listapropiedadDTO = null;
		HttpHeaders headers = new HttpHeaders();
		headers.setContentType(MediaType.APPLICATION_JSON);
		String token = userTokenRepository.obtenerToken(username);
		headers.set("Authorization", "Bearer " + token);
		HttpEntity<String> entity = new HttpEntity<String>(headers);
		Map<String, String> vars = new HashMap<>();
		vars.put("username", username);
		ResponseEntity<String> response = restTemplate.exchange("http://localhost:9090/propiedades/usuario/{username}",
				HttpMethod.GET, entity, String.class, vars);
		try {
			listapropiedadDTO = new ObjectMapper().readValue(response.getBody(),
					new TypeReference<List<PropiedadDTO>>() {
					});
		} catch (JsonMappingException e) {
			e.printStackTrace();
		} catch (JsonProcessingException e) {
			e.printStackTrace();
		}
		return listapropiedadDTO;

	}

	@PostMapping("/usuario/adicionarUsuario")
	public Map<String, String> adicionarUsuario(@RequestBody UsuarioDTO usuarioDTO) {

		Map<String, String> response = new HashMap<>();
		HttpHeaders headers = new HttpHeaders();
		headers.setContentType(MediaType.APPLICATION_JSON);
		HttpEntity<UsuarioDTO> entity = new HttpEntity<>(usuarioDTO, headers);
		ResponseEntity<String> responseApi = restTemplate.exchange("http://localhost:9090/usuario/adicionarUsuario",
				HttpMethod.POST, entity, String.class);

		if (responseApi.getStatusCode() == HttpStatus.OK) {
			response.put(Constantes.CODIGO_HTTP, "200");
			response.put(Constantes.MENSAJE_EXITO, "Registrado insertado exitosamente");
			return response;
		} else {
			response.put(Constantes.CODIGO_HTTP, "500");
			response.put(Constantes.MENSAJE_ERROR, "Ocurrió un problema al insertar");
			return response;
		}

	}

	@PostMapping("/usuarios/{username}/propiedad/adicionarPropiedad")
	public Map<String, String> adicionarPropiedadv2(@PathVariable(value = "username") String username,
			@RequestBody PropiedadDTO propiedadDTO) {

		Map<String, String> response = new HashMap<>();
		HttpHeaders headers = new HttpHeaders();
		headers.setContentType(MediaType.APPLICATION_JSON);
		String token = userTokenRepository.obtenerToken(username);
		headers.set("Authorization", "Bearer " + token);
		HttpEntity<PropiedadDTO> entity = new HttpEntity<>(propiedadDTO, headers);
		Map<String, String> vars = new HashMap<>();
		vars.put("username", username);
		ResponseEntity<String> responseApi = restTemplate.exchange(
				"http://localhost:9090/usuarios/{username}/propiedad/adicionarPropiedad", HttpMethod.POST, entity,
				String.class, vars);

		if (responseApi.getStatusCode() == HttpStatus.OK) {
			response.put(Constantes.CODIGO_HTTP, "200");
			response.put(Constantes.MENSAJE_EXITO, "Propiedad registrada exitosamente");
			return response;
		} else {
			response.put(Constantes.CODIGO_HTTP, "500");
			response.put(Constantes.MENSAJE_ERROR, "Ocurrió un problema al insertar");
			return response;
		}
	}

	@PostMapping("/propiedad/filtroavanzado/{username}")
	public List<PropiedadDTO> filtrarPropiedadesAvanzado(@PathVariable(value = "username") String username,
			@RequestBody List<PropiedadDTO> listaPropiedadDTO) {

		List<PropiedadDTO> listaPropiedadesDTO = null;
		HttpHeaders headers = new HttpHeaders();
		headers.setContentType(MediaType.APPLICATION_JSON);
		String token = userTokenRepository.obtenerToken(username);
		headers.set("Authorization", "Bearer " + token);
		
		HttpEntity<List<PropiedadDTO>> entity = new HttpEntity<>(listaPropiedadDTO, headers);
		ResponseEntity<String> responseApi = restTemplate.exchange("http://localhost:9090/propiedad/filtroavanzado",
				HttpMethod.POST, entity, String.class);
		try {
			listaPropiedadesDTO = new ObjectMapper().readValue(responseApi.getBody(),
					new TypeReference<List<PropiedadDTO>>() {
					});
			return listaPropiedadesDTO;
		} catch (JsonMappingException e) {
			e.printStackTrace();
			
			return listaPropiedadesDTO;
		} catch (JsonProcessingException e) {
			e.printStackTrace();
			return listaPropiedadesDTO;
		}
		

	}
	
	@GetMapping("/propiedades/filtro")
	public List<PropiedadDTO> filtrarPropiedades(@RequestParam Map<String, String> customQuery) {
		List<PropiedadDTO> listapropiedadDTO=null;
		HttpHeaders headers = new HttpHeaders();
		headers.setContentType(MediaType.APPLICATION_JSON);
		String token = userTokenRepository.obtenerToken(customQuery.get("username"));
		headers.set("Authorization", "Bearer " + token);
		
		HttpEntity<String> entity = new HttpEntity<>(headers);
		Map<String, String> vars = new HashMap<>();
		vars.put("area", customQuery.get("area"));
		vars.put("hab", customQuery.get("hab"));
		vars.put("valor", customQuery.get("valor"));
		ResponseEntity<String> responseApi = restTemplate.exchange("http://localhost:9090/propiedades/filtro?area={area}&hab={hab}&valor={valor}",
				HttpMethod.GET, entity, String.class,vars);
				
		try {
			listapropiedadDTO = new ObjectMapper().readValue(responseApi.getBody(),
					new TypeReference<List<PropiedadDTO>>() {
					});
		} catch (JsonMappingException e) {
			e.printStackTrace();
		} catch (JsonProcessingException e) {
			e.printStackTrace();
		}
		return listapropiedadDTO;
		

	}

}
