package br.com.pontointeligente.api.controllers;

import java.security.NoSuchAlgorithmException;

import javax.validation.Valid;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindingResult;
import org.springframework.validation.ObjectError;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import br.com.pontointeligente.api.dtos.CadastroPJDto;
import br.com.pontointeligente.api.entities.Empresa;
import br.com.pontointeligente.api.entities.Funcionario;
import br.com.pontointeligente.api.enums.PerfilEnum;
import br.com.pontointeligente.api.response.Response;
import br.com.pontointeligente.api.services.EmpresaService;
import br.com.pontointeligente.api.services.FuncionarioService;
import br.com.pontointeligente.api.utils.PasswordUtils;

@RestController
@RequestMapping("/api/cadastrar-pj")
@CrossOrigin(origins = "*")
public class CadastroPJController {

	private static final Logger log = LoggerFactory.getLogger(CadastroPJController.class);
	
	@Autowired
	private FuncionarioService funcionarioService;
	
	@Autowired
	private EmpresaService empresaService;
	
	/**
	 * Cadastra um Funcionário pessoa jurídica no sistema.
	 * 
	 * @param cadastroPJDto
	 * @param result
	 * @return
	 * @throws NoSuchAlgorithmException
	 */
	@PostMapping
	public ResponseEntity<Response<CadastroPJDto>> cadastrar(@Valid @RequestBody CadastroPJDto cadastroPJDto, BindingResult result) 
			throws NoSuchAlgorithmException {
		
		log.info("Cadastrando PJ {}", cadastroPJDto);
		
		Response<CadastroPJDto> response = new Response<>();
		
		validarDadosExistentes(cadastroPJDto, result);
		Empresa empresa = this.converterDtoParaEmpresa(cadastroPJDto);
		Funcionario funcionario = this.converterDtoParaFuncionario(cadastroPJDto, result);
		
		if (result.hasErrors()) {
			log.error("Erro validando dados de cadastro PJ: {}", result.getAllErrors());
			result.getAllErrors().forEach(error -> response.getErrors().add(error.getDefaultMessage()));
			return ResponseEntity.badRequest().body(response);
		}
		
		this.empresaService.persistir(empresa);
		funcionario.setEmpresa(empresa);
		this.funcionarioService.persistir(funcionario);
		
		response.setData(this.converterCadastroPJDto(funcionario));
		return ResponseEntity.ok(response);
	}

	/**
	 * Popula o DTO de cadastro com os dados do funcionário e empresa.
	 * 
	 * @param funcionario
	 * @return CadastroPJDto
	 */
	private CadastroPJDto converterCadastroPJDto(Funcionario funcionario) {
		CadastroPJDto cadastroPJDto = new CadastroPJDto();
		cadastroPJDto.setId(funcionario.getId());
		cadastroPJDto.setNome(funcionario.getNome());
		cadastroPJDto.setCnpj(funcionario.getEmpresa().getCnpj());
		cadastroPJDto.setCpf(funcionario.getCpf());
		cadastroPJDto.setEmail(funcionario.getEmail());
		cadastroPJDto.setRazaoSocial(funcionario.getEmpresa().getRazaoSocial());
		return cadastroPJDto;
	}

	private Funcionario converterDtoParaFuncionario(CadastroPJDto cadastroPJDto, BindingResult result)
			throws NoSuchAlgorithmException {
		
		Funcionario funcionario = new Funcionario();
		funcionario.setNome(cadastroPJDto.getNome());
		funcionario.setEmail(cadastroPJDto.getEmail());
		funcionario.setCpf(cadastroPJDto.getCpf());
		funcionario.setPerfil(PerfilEnum.ROLE_ADMIN);
		funcionario.setSenha(PasswordUtils.gerarBCrypt(cadastroPJDto.getSenha()));
		return funcionario;
	}

	private Empresa converterDtoParaEmpresa(CadastroPJDto cadastroPJDto) {
		Empresa empresa = new Empresa();
		empresa.setCnpj(cadastroPJDto.getCnpj());
		empresa.setRazaoSocial(cadastroPJDto.getRazaoSocial());
		return empresa;
	}

	/**
	 * Verifica se a empresa ou funcionário já existem na base de dados.	
	 * 
	 * @param cadastroPJDto
	 * @param result
	 */
	private void validarDadosExistentes(CadastroPJDto cadastroPJDto, BindingResult result) {
		this.empresaService.buscarPorCnpj(cadastroPJDto.getCnpj())
			.ifPresent(emp -> result.addError(new ObjectError("empresa", "Empresa já existente.")));
		
		this.funcionarioService.buscarPorCpf(cadastroPJDto.getCpf())
			.ifPresent(func -> result.addError(new ObjectError("funcionario", "CPF já existente.")));
		
		this.funcionarioService.buscarPorEmail(cadastroPJDto.getEmail())
			.ifPresent(func -> result.addError(new ObjectError("funcionario", "Email já existente.")));
	}
	
}
