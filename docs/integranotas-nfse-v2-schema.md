# IntegraNotas — schema do envio `POST /v2/nfse` (colado pelo usuário em 2026-11-24)

**Fonte:** schema colado pelo usuário a partir da documentação do IntegraNotas, em 2026-11-24 (knowledge-tier B). Texto
mantido como veio (não é JSON estrito: chaves sem aspas).

**Atenção:** este schema **não traz** `regime_apuracao` nem `nacional`, que o projeto envia e que a homologação confirmou
(ver `docs/integranotas-nfse-v2.md` e `C:\Projetos\tributacao-nfse-integranotas.md` §6 e §9). Ele parece ser o schema
genérico/municipal da v2, e não a variação nacional. Onde contradiz o que já foi homologado, vale o homologado; as
divergências estão listadas em `docs/integranotas-nfse-v2.md`, seção "Divergências com o schema de 2026-11-24".

```js
{
contingencia: {
  data: { required: true, type: "string", description: "Data e Hora da entrada em contingência", options: "ex. 2020-05-16T12:33:20-03:00" },
  motivo: { required: true, type: "string", description: "Motivo da entrada em contingência", options: "15 a 256 caracteres" }
},
identificacao: { required: false, type: ["string","null"], description: "Identificador unico enviado pelo sistema emissor para controle próprio.", options: "1 a 40 carateres" },
numero: { required: true, type: "string", description: "Número do RPS", options: "1 a 9 digitos" },
serie: { required: true, type: "string", description: "Série do RPS", options: "1 a 2 digitos" },
data_emissao: { required: true, type: "string", description: "Data de emissão do RPS", options: "ex. 2020-05-16T12:33:20-03:00" },
regime_tributacao: {
  required: true, type: "string", description: "Regime de Tributação. (situacao_tributaria)",
  options: [
    "0 - Tributada Integralmente (TI): O valor do imposto será lançado para o emissor da nota",
    "1 - Tributada Integralmente com ISSRF (TIRF): O valor do imposto não será lançado, pois será recolhido pelo tomador, caso seja órgão público municipal",
    "2 - Tributada Integralmente e sujeita à Substituição Tributária (TIST): O valor do imposto não será lançado, pois será recolhido pelo tomador (substituto tributário), caso não seja um órgão público municipal",
    "3 - Tributada com redução da base de cálculo (TRBC): O valor do imposto será lançado para o emissor da nota, porém, na apuração da base de cálculo, será descontado o valor da tag  (esta situação tributária somente se aplica, caso o serviço consignado seja o de código 1705)",
    "4 - Tributada com redução da base de cálculo com ISSRF (TRBCRF): O valor do imposto não será lançado, pois será recolhido pelo tomador, caso seja órgão público municipal, porém na apuração da base de cálculo será descontado o valor da tag  (esta situação tributária somente se aplica, caso o serviço consignado seja o de código 1705)",
    "5 - Tributada com redução da base de cálculo e sujeita à Substituição Tributária (TRBCST): O valor do imposto não será lançado, pois será recolhido pelo tomador, caso não seja um órgão público municipal, porém na apuração da base de cálculo será descontado o valor da tag  (esta situação tributária somente se aplica, caso o serviço consignado seja o de código 1705)",
    "6 - Isenta (ISE): Não irá gerar valor de imposto, pois o prestador é isento",
    "7 - Imune (IMU): Não irá gerar valor do imposto, pois o prestador é imune",
    "8 - Não Tributada - ISS regime Fixo (NTIFix): Não irá influenciar no cálculo do imposto, pois o valor é previamente calculado",
    "9 - Não Tributada - ISS regime Estimativa (NTIEs): Não irá influenciar no cálculo do imposto, pois o valor é previamente estimado",
    "10 - Não Tributada - ISS Construção Civil recolhido antecipadamente (NTICc): Não irá gerar valor de imposto, pois foi recolhido antecipadamente (esta situação tributária somente se aplica, caso os serviços consignados sejam os de código 1701, 1702, 1703, 1705, 1719)",
    "14 - Não tributada – (NTRIB): Não irá gerar valor do imposto, pois a prestação de serviço não está sujeita ao ISS",
    "15 - Não Tributada - Ato Cooperado (NTAC): Não irá gerar valor do imposto, pois a prestação de serviço para cooperados não está sujeita ao ISS, porém, mesmo que cooperativa e caso o serviço seja prestado para um não cooperado, deve-se utilizar das outras situações tributárias, de acordo com o caso"
  ]
},
tomador: {
  cnpj: { required: false, type: ["string","null"], description: "CNPJ do Tomador do Serviço", options: "14 digitos", rules: ["Informar cnpj, cpf ou nif"] },
  cpf: { required: false, type: ["string","null"], description: "CPF do Tomador do Serviço", options: "11 digitos", rules: ["Informar cnpj, cpf ou nif"] },
  nif: { required: false, type: ["string","null"], description: "Documento estrangeiro do Tomador do Serviço", options: "1 a 20 caracteres",
         rules: ["Informar cnpj, cpf ou nif", "Em caso de estrangeiro deve-se informar apenas a uf e codigo_pais na tag de endereco"] },
  ie: { required: false, type: ["string","null"], description: "Número da inscrição estadual do tomador do serviço", options: "1 a 15 caracteres" },
  razao_social: { required: true, type: "string", description: "Razão Social do Tomador do Serviço", options: "1 a 100 caracteres" },
  nome_fantasia: { required: false, type: ["string","null"], description: "Nome Fantasia do Tomador do Serviço", options: "1 a 100 caracteres" },
  email: { required: false, type: ["string","null"], maxLength: 60, description: "Email do Tomador do Serviço", options: "1 a 60 caracteres" },
  tipo_ente_governamental: { required: false, type: ["string","null"], description: "Tipo de ente governamental.",
         options: ["1 - União", "2 - Estado", "3 - Distrito Federal", "4 - Município"] },
  endereco: {
    endereco_alternativo: { required: false, type: ["boolean","null"], description: "Indica se vai ser utilizado o endereço alternativo", options: ["true - SIM", "false - Não"], default: "true" },
    logradouro: { required: true, type: "string", description: "Rua (Logradouro) do Tomador do Serviço", options: "1 a 70 caracteres" },
    numero: { required: true, type: "string", maxLength: 8, description: "Número do Endereço do Tomador do Serviço", options: "1 a 8 caracteres" },
    complemento: { required: false, type: ["string","null"], description: "Complemento do Endereço do Tomador do Serviço", options: "1 a 50 caracteres" },
    ponto_referencia: { required: false, type: ["string","null"], description: "Ponto de referência do endereço do tomador", options: "1 a 50 caracteres" },
    bairro: { required: true, type: "string", description: "Bairro do Endereço do Tomador do Serviço", options: "1 a 100 caracteres" },
    nome_provincia: { required: false, type: ["string","null"], description: "Nome do estado de origem do tomador estrangeiro", options: "5 a 100 caracteres" },
    nome_municipio: { required: false, type: ["string","null"], description: "Nome da cidade de origem do tomador estrangeiro", options: "5 a 60 caracteres" },
    codigo_municipio: { required: false, type: ["string","null"], description: "Código IBGE do Municipio do Endereço do Tomador do Serviço", options: "7 digitos" },
    uf: { required: true, type: "string", description: "Sigla do Estado do Endereço do Tomador do Serviço", options: "2 caracteres", rules: ["Para exportação usar \"EX\""] },
    cep: { required: false, type: ["string","null"], description: "Codigo Postal do Tomador do Serviço", options: "8 digitos", rules: ["Se exportação Não passar esse dado ou deixe null"] },
    codigo_pais: { required: false, type: ["string","null"], description: "Código do país onde o serviço foi prestado", options: "3 ou 4 digitos", rules: ["Indicar apenas se for uma exportação de serviço"] }
  }
},
servico: {
  endereco_local_prestacao: {
    codigo_municipio: { required: true, type: "string", description: "Código IBGE do município onde o serviço foi prestado", options: "7 digitos" },
    codigo_pais: { required: false, type: ["string","null"], description: "Código do país onde o serviço foi prestado", options: "4 digitos", rules: ["Indicar apenas se for uma exportação de serviço"] }
  },
  codigo: { required: true, type: "string", description: "Código do serviço prestado Item da LC 116/2003", options: "3 a 6 digitos" },
  codigo_tributacao_municipio: { required: false, type: ["string","null"], description: "Código do serviço prestado próprio do município", options: "3 digitos", rules: ["Consultar lista de códigos do seu municipio"] },
  codigo_cnae: { required: false, type: ["string","null"], description: "Código CNAE para o Serviço Prestado. (codigo_atividade)", options: "9 digitos" },
  codigo_nbs: { required: true, type: "string", description: "Código da lista de Nomenclatura Brasileira de Serviços", options: "9 digitos" },
  discriminacao: { required: true, type: "string", maxLength: 1000, description: "Discriminação dos serviços", options: "1 a 1000 caracteres", rules: ["Para quebra de linha use \n"] },
  codigo_interno: { required: false, type: ["string","null"], description: "Codigo da unidade dos serviços pré-cadastrados. Vide documentação da sua prefeitura", options: "1 a 9 digitos" },
  valor_servicos: { required: false, type: ["number","null"], minimum: 0.01, description: "Valor dos serviços em R$", options: "ex. 100.00" },
  valor_desconto_incondicionado: { required: false, type: ["number","null"], minimum: 0, description: "Valor do desconto incondicionado", options: "ex. 100.00" },
  tributos_municipais: {
    iss_retido: { required: false, type: ["boolean","null"], description: "ISS foi retido na fonte", options: ["true - SIM", "false - Não"],
                  rules: ["Normalmente ocorre apenas quando o serviço realizado fora do municipio, vide legislação da cidade"], default: "false" },
    aliquota_iss: { required: true, type: "number", minimum: 0, maximum: 100, description: "Alíquota do serviço prestado", options: "ex. 5.00",
                    rules: ["Deve ser sempre enviado esse valor em percentual, 5% seria enviado como 5.00 e nao 0.05"] },
    valor_base_calculo_iss: { required: true, type: "number", minimum: 0, description: "Valor da base de calculo tributável da NFse", options: "ex. 100.00" },
    valor_iss_retido: { required: false, type: ["number","null"], minimum: 0, description: "Valor do ISS retido na fonte em R$", options: "ex. 100.00" }
  },
  tributos_nacionais: {
    cst: {
      required: true, type: "string", description: "Situação tributária do PIS e COFINS.",
      options: [
        "01 - operação tributável - base de cálculo = valor da operação (alíquota normal - cumulativo/não cumulativo)",
        "02 - operação tributável: base de cálculo = valor da operação (alíquota diferenciada)",
        "03 - operação tributável: base de cálculo = quantidade vendida × alíquota por unidade de produto",
        "04 - operação tributável: tributação monofásica (alíquota zero)",
        "05 - operação tributável: substituição tributária",
        "06 - operação tributável: alíquota zero",
        "07 - operação isenta da contribuição",
        "08 - operação sem incidência da contribuição",
        "09 - operação com suspensão da contribuição",
        "49 - outras operações de saída",
        "50 - operação com direito a crédito: vinculada exclusivamente a receita tributada no mercado interno",
        "51 - operação com direito a crédito: vinculada exclusivamente a receita não tributada no mercado interno",
        "52 - operação com direito a crédito: vinculada exclusivamente a receita de exportação",
        "53 - operação com direito a crédito: vinculada a receitas tributadas e não-tributadas no mercado interno",
        "54 - operação com direito a crédito: vinculada a receitas tributadas no mercado interno e de exportação",
        "55 - operação com direito a crédito: vinculada a receitas não-tributadas no mercado interno e de exportação",
        "56 - operação com direito a crédito: vinculada a receitas tributadas e não-tributadas no mercado interno e de exportação",
        "60 - crédito presumido: operação de aquisição vinculada exclusivamente a receita tributada no mercado interno",
        "61 - crédito presumido: operação de aquisição vinculada exclusivamente a receita não-tributada no mercado interno",
        "62 - crédito presumido: operação de aquisição vinculada exclusivamente a receita de exportação",
        "63 - crédito presumido: operação de aquisição vinculada a receitas tributadas e não-tributadas no mercado interno",
        "64 - crédito presumido: operação de aquisição vinculada a receitas tributadas no mercado interno e de exportação",
        "65 - crédito presumido: operação de aquisição vinculada a receitas não-tributadas no mercado interno e de exportação",
        "66 - crédito presumido: operação de aquisição vinculada a receitas tributadas e não-tributadas no mercado interno e de exportação",
        "67 - crédito presumido: outras operações",
        "70 - operação de aquisição sem direito a crédito",
        "71 - operação de aquisição com isenção",
        "72 - operação de aquisição com suspensão",
        "73 - operação de aquisição a alíquota zero",
        "74 - operação de aquisição sem incidência da contribuição",
        "75 - operação de aquisição por substituição tributária",
        "98 - outras operações de entrada",
        "99 - outras operações"
      ]
    },
    tipo_retencao: {
      required: false, type: ["string","null"], description: "Informa se o PIS/COFINS foi retido.",
      options: [
        "0 - PIS/COFINS/CSLL Não Retidos", "1 - PIS/COFINS Retido", "2 - PIS/COFINS Não Retido", "3 - PIS/COFINS/CSLL Retidos",
        "4 - PIS/COFINS Retidos, CSLL Não Retido", "5 - PIS Retido, COFINS/CSLL Não Retido", "6 - COFINS Retido, PIS/CSLL Não Retido",
        "7 - PIS Não Retido, COFINS/CSLL Retidos", "8 - PIS/COFINS Não Retidos, CSLL Retido", "9 - COFINS Não Retido, PIS/CSLL Retidos"
      ],
      default: "2 - PIS/COFINS Não Retido"
    },
    valor_base_calculo: { required: true, type: "number", minimum: 0, description: "Valor de base de cálculo PIS e COFINS.", options: "ex. 100.00" },
    aliquota_pis: { required: false, type: ["number","null"], minimum: 0, description: "Percentual PIS.", options: "ex. 10.00" },
    aliquota_cofins: { required: false, type: ["number","null"], minimum: 0, description: "Percentual COFINS.", options: "ex. 10.00" },
    valor_pis: { required: false, type: ["number","null"], minimum: 0, description: "Valor da retenção do PIS em R$ Informação declaratória", options: "ex. 100.00" },
    valor_cofins: { required: false, type: ["number","null"], minimum: 0, description: "Valor da retenção do COFINS em R$ Informação declaratória", options: "ex. 100.00" },
    valor_inss: { required: false, type: ["number","null"], minimum: 0, description: "Valor da retenção do INSS em R$ Informação declaratória", options: "ex. 100.00" },
    valor_ir: { required: false, type: ["number","null"], minimum: 0, description: "Valor da retenção do IR em R$ Informação declaratória", options: "ex. 100.00" },
    valor_csll: { required: false, type: ["number","null"], minimum: 0, description: "Valor da retenção do CSLL em R$ Informação declaratória", options: "ex. 100.00" },
    valor_previdencia_social: { required: false, type: ["number","null"], minimum: 0, description: "Valor das retençoes da previdêncial social", options: "ex. 100.00" }
  },
  ibs_cbs: {
    codigo_municipio_incidencia: { required: true, type: "string", description: "Código IBGE da localidade de incidência do IBS/CBS (local da operação)", options: "7 digitos" },
    consumidor_final: { required: true, type: "string", description: "Indica operação de uso ou consumo pessoal (art. 57).", options: ["0 - Não", "1 - Sim"] },
    codigo_credito_presumido: { required: false, type: ["string","null"], description: "Código e Classificação do Crédito Presumido: IBS e CBS.", options: "2 digitos" },
    codigo_fornecimento: { required: true, type: "string", description: "Código indicador da operação de fornecimento, conforme tabela \"código indicador de operação\".", options: "6 digitos" },
    tipo_operacao: {
      required: false, type: ["string","null"], description: "Tipo de Operação com Entes Governamentais ou outros serviços sobre bens imóveis.",
      options: [
        "1 - Fornecimento com pagamento posterior", "2 - Recebimento do pagamento com fornecimento já realizado",
        "3 - Fornecimento com pagamento já realizado", "4 - Recebimento do pagamento com fornecimento posterior",
        "5 - Fornecimento e recebimento do pagamento concomitantes"
      ]
    },
    nfse_referenciadas: [ { chave: { required: true, type: "string", description: "Tipo Chave da Nota Fiscal de Serviço Eletrônica.", options: "50 digitos" } } ],
    situacao_tributaria: { required: true, type: "string", description: "Código de Situação Tributária do IBS e da CBS.", options: "3 digitos" },
    classificacao_tributaria: { required: true, type: "string", description: "Código de Classificação Tributária do IBS e da CBS.", options: "6 digitos" },
    diferimento: {
      percentual_ibs_uf: { required: true, type: "number", description: "Percentual de diferimento para o IBS estadual.", options: "ex. 8.00" },
      percentual_ibs_municipio: { required: true, type: "number", description: "Percentual de diferimento para o IBS municipal.", options: "ex. 8.00" },
      percentual_cbs: { required: true, type: "number", description: "Percentual de diferimento para a CBS.", options: "ex. 8.00" }
    },
    imovel: {
      numero_matricula: { required: false, type: ["string","null"], description: "Inscrição imobiliária fiscal (código fornecido pela Prefeitura Municipal para a identificação da obra ou para fins de recolhimento do IPTU)", options: "1 a 30 caracteres" },
      codigo_cadastro_imobiliario: { required: false, type: ["string","null"], description: "Código do Cadastro Imobiliário Brasileiro - CIB.", options: "8 caracteres" },
      endereco: {
        logradouro: { required: true, type: "string", description: "Rua (Logradouro).", options: "1 a 255 caracteres" },
        numero: { required: true, type: "string", description: "Número do Endereço.", options: "1 a 60 caracteres" },
        complemento: { required: false, type: ["string","null"], description: "Complemento do Endereço.", options: "1 a 255 caracteres" },
        bairro: { required: true, type: "string", description: "Bairro do Endereço.", options: "1 a 60 caracteres" },
        uf: { required: true, type: "string", description: "Sigla da UF", options: "2 caracteres", rules: ["Em caso de exportação informar EX"] },
        cep: { required: false, type: ["string","null"], description: "Codigo Postal.", options: "8 digitos" },
        codigo_postal: { required: false, type: ["string","null"], description: "Codigo Postal. Declarar somente quando for fora do Brasil (Exportação).", options: "1 a 11 caracteres" },
        nome_municipio: { required: false, type: ["string","null"], description: "Nome do municipio. Declarar somente quando for fora do Brasil (Exportação).", options: "1 a 60 caracteres" },
        nome_provincia: { required: false, type: ["string","null"], description: "Nome da região/provincia. Declarar somente quando for fora do Brasil (Exportação). (xEstProvReg)", options: "1 a 60 caracteres" }
      }
    }
  }
},
deducoes: { valor: { required: true, type: "number", minimum: 0, description: "Valor da dedução.", options: "ex. 100.00" } },
pedagio: { codigo_equipamento: { required: true, type: "string", description: "Codigo do equipamento de pedágio", options: "1 a 100 caracteres" } },
obra: { codigo: { required: true, type: "string", description: "Número da matricula CEI da obra ou da empresa", options: "12 digitos" } },
informacoes_complementares: { required: false, type: ["string","null"], description: "Informações complementares", options: "1 à 1000 caracteres" }
}
```
