package br.com.pontointeligente.api.services;

import java.util.Optional;

import br.com.pontointeligente.api.entities.Empresa;

public interface EmpresaService {

	Optional<Empresa> buscarPorCnpj(String cnpj);

	Empresa persistir(Empresa empresa);
	
}
