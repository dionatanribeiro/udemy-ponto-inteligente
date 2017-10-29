package br.com.pontointeligente.api.controllers;

import java.math.BigDecimal;
import java.security.NoSuchAlgorithmException;
import java.util.Optional;

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

import br.com.pontointeligente.api.dtos.CadastroPFDto;
import br.com.pontointeligente.api.entities.Empresa;
import br.com.pontointeligente.api.entities.Funcionario;
import br.com.pontointeligente.api.enums.PerfilEnum;
import br.com.pontointeligente.api.response.Response;
import br.com.pontointeligente.api.services.EmpresaService;
import br.com.pontointeligente.api.services.FuncionarioService;
import br.com.pontointeligente.api.utils.PasswordUtils;

@RestController
@RequestMapping("/api/cadastrar-pf")
@CrossOrigin("*")
public class CadastroPFController {

	private static final Logger log = LoggerFactory.getLogger(CadastroPJController.class);

	@Autowired
	private EmpresaService empresaService;
	
	@Autowired
	private FuncionarioService funcionarioService;
	
	/**
	 * Cadastra um Funcionário pessoa física no sistema.
	 * 
	 * @param cadastroPFDto
	 * @param result
	 * @return
	 * @throws NoSuchAlgorithmException
	 */
	@PostMapping
	public ResponseEntity<Response<CadastroPFDto>> cadastrar(@Valid @RequestBody CadastroPFDto cadastroPFDto, BindingResult result)
			throws NoSuchAlgorithmException {
		// TODO Auto-generated method stub

		log.info("Cadastrando PF: {}", cadastroPFDto);
		Response<CadastroPFDto> response = new Response<CadastroPFDto>();
		
		validarDadosExistentes(cadastroPFDto, result);
		Funcionario funcionario = this.converterDtoParaFuncionario(cadastroPFDto, result);
		
		if (result.hasErrors()) {
			log.error("Erro validando dados de cadastro PF: {}", result.getAllErrors());
			result.getAllErrors().forEach(error -> response.getErrors().add(error.getDefaultMessage()));
			return ResponseEntity.badRequest().body(response);
		}
		
		Optional<Empresa> empresa = this.empresaService.buscarPorCnpj(cadastroPFDto.getCnpj());
		empresa.ifPresent(funcionario::setEmpresa);
		this.funcionarioService.persistir(funcionario);
		
		response.setData(this.converterCadastroPFDto(funcionario));
		return ResponseEntity.ok(response);
	}

	private CadastroPFDto converterCadastroPFDto(Funcionario funcionario) {
		CadastroPFDto cadastroPFDto = new CadastroPFDto();
		cadastroPFDto.setId(funcionario.getId());
		cadastroPFDto.setNome(funcionario.getNome());
		cadastroPFDto.setCnpj(funcionario.getEmpresa().getCnpj());
		cadastroPFDto.setCpf(funcionario.getCpf());
		cadastroPFDto.setEmail(funcionario.getEmail());
		
		funcionario.getQtdHorasAlmocoOpt()
			.ifPresent(qtdHorasAlmoco -> cadastroPFDto.setQtdHorasAlmoco(Optional.of(Float.toString(qtdHorasAlmoco))));
	
		funcionario.getQtdHorasTrabalhoDiaOpt()
			.ifPresent(qtdHorasTrabalhoDia -> cadastroPFDto.setQtdHorasTrabalhoDia(Optional.of(Float.toString(qtdHorasTrabalhoDia))));
	
		funcionario.getValorHoraOpt()
			.ifPresent(valorHora -> cadastroPFDto.setValorHora(Optional.of(valorHora.toString())));
		
		return cadastroPFDto;
	}

	private Funcionario converterDtoParaFuncionario(CadastroPFDto cadastroPFDto, BindingResult result)
			throws NoSuchAlgorithmException {
		Funcionario funcionario = new Funcionario();
		
		funcionario.setNome(cadastroPFDto.getNome());
		funcionario.setEmail(cadastroPFDto.getEmail());
		funcionario.setCpf(cadastroPFDto.getCpf());
		funcionario.setPerfil(PerfilEnum.ROLE_USUARIO);
		funcionario.setSenha(PasswordUtils.gerarBCrypt(cadastroPFDto.getSenha()));
		
		cadastroPFDto.getQtdHorasAlmoco()
			.ifPresent(qtdHorasAlmoco -> funcionario.setQtdHorasAlmoco(Float.valueOf(qtdHorasAlmoco)));
		
		cadastroPFDto.getQtdHorasTrabalhoDia()
			.ifPresent(qtdHorasTrabalhoDia -> funcionario.setQtdHorasTrabalhoDia(Float.valueOf(qtdHorasTrabalhoDia)));
		
		cadastroPFDto.getValorHora()
			.ifPresent(valorHora -> funcionario.setValorHora(new BigDecimal(valorHora)));
		
		return funcionario;
	}

	/**
	 * Verifica se a empresa está cadastrada e se o funcionário não existe na base de dados.
	 * 
	 * @param cadastroPFDto
	 * @param result
	 */
	private void validarDadosExistentes(CadastroPFDto cadastroPFDto, BindingResult result) {
		Optional<Empresa> empresa = this.empresaService.buscarPorCnpj(cadastroPFDto.getCnpj());
		if (!empresa.isPresent()) {
			result.addError(new ObjectError("empresa", "Empresa não cadastrada."));
		}
		
		this.funcionarioService.buscarPorCpf(cadastroPFDto.getCpf())
			.ifPresent(func -> result.addError(new ObjectError("funcionario", "CPF já existente")));

		this.funcionarioService.buscarPorEmail(cadastroPFDto.getEmail())
		.ifPresent(func -> result.addError(new ObjectError("funcionario", "Email já existente")));
		
		
		
	}
	
}
