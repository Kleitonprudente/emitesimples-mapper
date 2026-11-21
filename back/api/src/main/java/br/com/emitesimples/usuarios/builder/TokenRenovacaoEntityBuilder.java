package br.com.emitesimples.usuarios.builder;

import java.time.LocalDateTime;
import br.com.emitesimples.usuarios.domain.TokenRenovacao;
import br.com.emitesimples.usuarios.entity.TokenRenovacaoEntity;
import java.util.List;
import java.util.ArrayList;
import br.com.emitesimples.usuarios.entity.UsuarioEntity;
import br.com.emitesimples.configuracao.entity.EmpresaEntity;

/*
 * TokenRenovacao entity builder
*/
public class TokenRenovacaoEntityBuilder {
    public LocalDateTime version;
    public String tokenHash;
    public LocalDateTime dataExpiracao;
    public Boolean usado;
    public Boolean revogado;
    public LocalDateTime dataUso;
    public LocalDateTime dataRevogacao;
    public String id;
    public LocalDateTime dataCriacao;
    public LocalDateTime dataAtualizacao;
    public UsuarioEntity usuario;
    public EmpresaEntity empresa;

    public TokenRenovacaoEntityBuilder() {
    }
    public TokenRenovacaoEntityBuilder withVersion(LocalDateTime version) {
        this.version = version;
        return this;
    }

    public TokenRenovacaoEntityBuilder withTokenHash(String tokenHash) {
        this.tokenHash = tokenHash;
        return this;
    }

    public TokenRenovacaoEntityBuilder withDataExpiracao(LocalDateTime dataExpiracao) {
        this.dataExpiracao = dataExpiracao;
        return this;
    }

    public TokenRenovacaoEntityBuilder withUsado(Boolean usado) {
        this.usado = usado;
        return this;
    }

    public TokenRenovacaoEntityBuilder withRevogado(Boolean revogado) {
        this.revogado = revogado;
        return this;
    }

    public TokenRenovacaoEntityBuilder withDataUso(LocalDateTime dataUso) {
        this.dataUso = dataUso;
        return this;
    }

    public TokenRenovacaoEntityBuilder withDataRevogacao(LocalDateTime dataRevogacao) {
        this.dataRevogacao = dataRevogacao;
        return this;
    }

    public TokenRenovacaoEntityBuilder withId(String id) {
        this.id = id;
        return this;
    }

    public TokenRenovacaoEntityBuilder withDataCriacao(LocalDateTime dataCriacao) {
        this.dataCriacao = dataCriacao;
        return this;
    }

    public TokenRenovacaoEntityBuilder withDataAtualizacao(LocalDateTime dataAtualizacao) {
        this.dataAtualizacao = dataAtualizacao;
        return this;
    }

    public TokenRenovacaoEntityBuilder withUsuario(UsuarioEntity usuario) {
        this.usuario = usuario;
        return this;
    }

    public TokenRenovacaoEntityBuilder withEmpresa(EmpresaEntity empresa) {
        this.empresa = empresa;
        return this;
    }

    public TokenRenovacaoEntity build() {
        return new TokenRenovacaoEntity(this);
    }

    public TokenRenovacaoEntityBuilder withModel(TokenRenovacao tokenRenovacao) {
        return this.withVersion(tokenRenovacao.getVersion())
                .withDataExpiracao(tokenRenovacao.getDataExpiracao())
                .withUsado(tokenRenovacao.getUsado())
                .withRevogado(tokenRenovacao.getRevogado())
                .withDataUso(tokenRenovacao.getDataUso())
                .withDataRevogacao(tokenRenovacao.getDataRevogacao())
                .withId(tokenRenovacao.getId())
                .withDataCriacao(tokenRenovacao.getDataCriacao())
                .withDataAtualizacao(tokenRenovacao.getDataAtualizacao())
                .withUsuario(UsuarioEntity.builderOf().withModel(tokenRenovacao.getUsuario()).build())
                .withEmpresa(EmpresaEntity.builderOf().withModel(tokenRenovacao.getEmpresa()).build());
    }

    public List<TokenRenovacaoEntity> withModelList(List<TokenRenovacao> tokenRenovacaoList) {
        if (tokenRenovacaoList == null) {
            return null;
        }

        List<TokenRenovacaoEntity> list = new ArrayList<TokenRenovacaoEntity>(tokenRenovacaoList.size());
        for (TokenRenovacao tokenRenovacao: tokenRenovacaoList) {
           list.add(withModel(tokenRenovacao).build());
        }
        return list;
    }

}
