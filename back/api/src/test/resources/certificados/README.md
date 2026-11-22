# Certificados de teste (PFX)

Arquivos **de teste**, sem valor: chaves geradas so para os testes (`CertificadoTest`). Nunca use certificado real aqui.
Todos com senha `senha123`, exceto onde dito. Gerados com o `keytool` do JDK (PKCS12, RSA 2048, SHA256withRSA):

```sh
gen() { keytool -genkeypair -alias cert -keyalg RSA -keysize 2048 -sigalg SHA256withRSA -storetype PKCS12 \
          -keystore "$1" -storepass "$2" -keypass "$2" -dname "$3" "${@:4}"; }

gen valido.pfx        senha123  "CN=EMPRESA TESTE:27865757000102, OU=Teste, O=ICP-Brasil, C=BR" -validity 36500
gen vencido.pfx       senha123  "CN=EMPRESA TESTE:27865757000102, O=ICP-Brasil, C=BR" -startdate "2020/01/01 00:00:00" -validity 30
gen outro-cnpj.pfx    senha123  "CN=OUTRA EMPRESA:11222333000181, O=ICP-Brasil, C=BR" -validity 36500
gen sem-cnpj.pfx      senha123  "CN=EMPRESA SEM DOCUMENTO, O=Teste, C=BR" -validity 36500
gen senha-simbolo.pfx 'senha@123' "CN=EMPRESA TESTE:27865757000102, O=ICP-Brasil, C=BR" -validity 36500
gen futuro.pfx        senha123  "CN=EMPRESA TESTE:27865757000102, O=ICP-Brasil, C=BR" -startdate "+3650d" -validity 365
head -c 2048 /dev/urandom > nao-pkcs12.pfx      # bytes aleatorios: nao e PKCS12
```

| Arquivo | Para que serve |
|---|---|
| `valido.pfx` | CNPJ `27865757000102` no CN, validade de 100 anos |
| `vencido.pfx` | `notAfter` em 2020 -> `certificado_vencido` |
| `outro-cnpj.pfx` | CNPJ `11222333000181` -> `certificado_nao_pertence` |
| `sem-cnpj.pfx` | nenhum CNPJ no titular -> aceito com aviso `nao_foi_possivel_conferir_cnpj` |
| `senha-simbolo.pfx` | senha `senha@123` (com simbolo): a API recusa **antes** de abrir o arquivo (`certificado_senha_invalida`) |
| `futuro.pfx` | `notBefore` daqui a 10 anos -> `certificado_invalido` |
| `nao-pkcs12.pfx` | lixo aleatorio -> `certificado_invalido` |

Os testes ajustam o `documento` da empresa para `27865757000102` antes de enviar o `valido.pfx`.
