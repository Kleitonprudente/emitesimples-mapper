import { beforeEach, describe, expect, it, vi } from 'vitest'
import { render, renderHook, screen, waitFor } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import type { ReactNode } from 'react'
import { ApiError } from '@/lib/api'
import { EmpresaConfig } from '@/features/empresa-config/EmpresaConfig'
import { useEnviarCertificado } from '@/features/empresa-config/empresaApi'

const apiMock = vi.hoisted(() => vi.fn())
const toastMock = vi.hoisted(() => ({ success: vi.fn(), error: vi.fn() }))
const sessao = vi.hoisted(() => ({ atual: null as unknown }))
vi.mock('@/lib/api', async (orig) => ({ ...(await orig<typeof import('@/lib/api')>()), api: apiMock }))
vi.mock('@/lib/auth', async (orig) => ({ ...(await orig<typeof import('@/lib/auth')>()), useSession: () => sessao.atual }))
vi.mock('sonner', () => ({ toast: toastMock }))

const URL_E = '/configuracao/empresa'
const URL_C = '/configuracao/empresa/certificado'
const NAO_ENVIADO = { situacao: 'nao_enviado', validade: null, pendenteDeEnvio: false, venceEmDias: null }
const EMPRESA = {
  razaoSocial: 'Horizonte Serviços Contábeis Ltda',
  nomeFantasia: 'Horizonte Contábil',
  documento: '11222333000181',
  inscricaoMunicipal: '12345',
  rgInscricaoEstadual: null,
  email: 'contato@horizonte.com.br',
  whatsapp: '11987654321',
  telefone: null,
  cep: '85980000',
  logradouro: 'Rua Quinze de Novembro',
  numeroEndereco: '100',
  complemento: null,
  bairro: 'Centro',
  municipio: { id: 'mun-4108809', nome: 'Guaíra', uf: 'PR', codigoIbge: '4108809' },
  regimeTributario: 1,
  regimeIbsCbs: null,
  documentoEditavel: true,
  certificado: NAO_ENVIADO,
}

type Resposta = unknown | (() => Promise<unknown>)
function prepara(get: unknown = EMPRESA, put?: Resposta, post?: Resposta) {
  const resolve = async (r: Resposta | undefined, padrao: unknown) => (typeof r === 'function' ? (r as () => Promise<unknown>)() : (r ?? padrao))
  apiMock.mockImplementation(async (p: string, opts?: { method?: string }) => {
    if (opts?.method === 'PUT') return resolve(put, get)
    if (opts?.method === 'POST' && p === URL_C) return resolve(post, { ...NAO_ENVIADO, situacao: 'valido', avisos: [] })
    return get
  })
}
const falhaCom = (e: ApiError) => async () => {
  throw e
}
const puts = () => apiMock.mock.calls.filter(([, o]) => o?.method === 'PUT')
const posts = () => apiMock.mock.calls.filter(([, o]) => o?.method === 'POST')
const gets = () => apiMock.mock.calls.filter(([, o]) => !o?.method)

function renderTela() {
  const qc = new QueryClient({ defaultOptions: { queries: { retry: false } } })
  render(
    <QueryClientProvider client={qc}>
      <EmpresaConfig />
    </QueryClientProvider>,
  )
}

const empresa = { id: 'emp-1', razaoSocial: 'Horizonte' }
const comPapel = (papel: string | null, plataformaAdmin = false) => ({ usuario: { id: 'u', nome: 'Ana', email: 'a@x.com' }, empresa: papel ? empresa : null, papel, plataformaAdmin })

const razao = () => screen.findByLabelText(/Razão social/)
const salvar = () => screen.getByRole('button', { name: 'Salvar' })
const enviarBtn = () => screen.getByRole('button', { name: 'Enviar certificado' })
const campoArquivo = () => screen.getByLabelText('Arquivo do certificado (.pfx)') as HTMLInputElement
const campoSenha = () => screen.getByLabelText('Senha do certificado') as HTMLInputElement
const pfx = (nome = 'empresa.pfx', bytes = 100) => new File([new Uint8Array(bytes)], nome)

async function enviaCertificado(user: ReturnType<typeof userEvent.setup>, nome = 'empresa.pfx', senha = 'Senha123') {
  await razao()
  await user.upload(campoArquivo(), pfx(nome))
  await user.type(campoSenha(), senha)
  await user.click(enviarBtn())
}

beforeEach(() => {
  apiMock.mockReset()
  toastMock.success.mockReset()
  sessao.atual = comPapel('administrador')
})

describe('EmpresaConfig: dados', () => {
  it.each([
    ['operador', comPapel('operador')],
    ['consulta', comPapel('consulta')],
    ['plataforma sem empresa', comPapel(null, true)],
  ])('%s ve acesso restrito e nao chama a API', (_n, s) => {
    sessao.atual = s
    renderTela()
    expect(screen.getByRole('alert')).toHaveTextContent('Acesso restrito')
    expect(apiMock).not.toHaveBeenCalled()
  })

  it('carrega e mostra as secoes, os valores e o municipio', async () => {
    prepara()
    renderTela()
    expect(await razao()).toHaveValue('Horizonte Serviços Contábeis Ltda')
    expect(apiMock).toHaveBeenCalledWith(URL_E)
    for (const s of ['Identificação', 'Contato', 'Endereço', 'Tributação']) expect(screen.getByText(s)).toBeInTheDocument()
    expect(screen.getByLabelText(/CNPJ/)).toHaveValue('11.222.333/0001-81')
    expect(screen.getByLabelText(/Inscrição municipal/)).toHaveValue('12345')
    expect(screen.getByLabelText(/WhatsApp/)).toHaveValue('(11) 98765-4321')
    expect(screen.getByLabelText(/^CEP/)).toHaveValue('85980-000')
    expect(screen.getByText('Guaíra - PR')).toBeInTheDocument()
    expect(screen.getByRole('combobox', { name: /Regime tributário/ })).toHaveTextContent('Simples Nacional')
    expect(screen.queryByText(/não pode ser alterado/)).not.toBeInTheDocument()
    expect(screen.getByLabelText(/CNPJ/)).toBeEnabled()
  })

  it('mostra erro de carregamento', async () => {
    apiMock.mockRejectedValue(new ApiError(500, 'erro', 'Falha.'))
    renderTela()
    expect(await screen.findByText('Não foi possível carregar os dados da empresa.')).toBeInTheDocument()
  })

  it('documento nao editavel: somente leitura com explicacao, e o PUT nao leva o documento', async () => {
    prepara({ ...EMPRESA, documentoEditavel: false })
    const user = userEvent.setup()
    renderTela()
    await razao()
    expect(screen.getByLabelText(/CNPJ/)).toBeDisabled()
    expect(screen.getByText('O CNPJ não pode ser alterado depois do cadastro na IntegraNotas.')).toBeInTheDocument()
    await user.click(salvar())
    await waitFor(() => expect(puts()).toHaveLength(1))
    expect(puts()[0]![1].body).not.toHaveProperty('documento')
  })

  it('salva o payload certo (nulos para opcionais vazios, municipio {id}), avisa e relê', async () => {
    prepara()
    const user = userEvent.setup()
    renderTela()
    const campo = await razao()
    await user.clear(campo)
    await user.type(campo, 'Horizonte Contábil Ltda')
    await user.click(salvar())
    await waitFor(() => expect(puts()).toHaveLength(1))
    expect(puts()[0]).toEqual([
      URL_E,
      {
        method: 'PUT',
        body: {
          razaoSocial: 'Horizonte Contábil Ltda',
          nomeFantasia: 'Horizonte Contábil',
          documento: '11222333000181',
          inscricaoMunicipal: '12345',
          rgInscricaoEstadual: null,
          email: 'contato@horizonte.com.br',
          whatsapp: '11987654321',
          telefone: null,
          cep: '85980000',
          logradouro: 'Rua Quinze de Novembro',
          numeroEndereco: '100',
          complemento: null,
          bairro: 'Centro',
          municipio: { id: 'mun-4108809' },
          regimeTributario: 1,
          regimeIbsCbs: null,
        },
      },
    ])
    await waitFor(() => expect(toastMock.success).toHaveBeenCalledWith('Dados da empresa salvos.'))
    await waitFor(() => expect(gets().length).toBeGreaterThanOrEqual(2))
  })

  it('campo obrigatorio vazio, e-mail e CNPJ invalidos sao barrados sem enviar', async () => {
    prepara()
    const user = userEvent.setup()
    renderTela()
    await user.clear(await razao())
    await user.clear(screen.getByLabelText(/E-mail/))
    await user.type(screen.getByLabelText(/E-mail/), 'x')
    await user.clear(screen.getByLabelText(/CNPJ/))
    await user.type(screen.getByLabelText(/CNPJ/), '12345678000100')
    await user.click(salvar())
    expect(await screen.findByText('Campo obrigatório')).toBeInTheDocument()
    expect(screen.getByText('E-mail inválido.')).toBeInTheDocument()
    expect(screen.getByText('CNPJ inválido.')).toBeInTheDocument()
    expect(puts()).toHaveLength(0)
  })

  describe('erros do back ao salvar', () => {
    async function falha(e: ApiError) {
      prepara(EMPRESA, falhaCom(e))
      const user = userEvent.setup()
      renderTela()
      await razao()
      await user.click(salvar())
    }
    const alerta = async (texto: string) => expect(await screen.findByText(texto)).toBeInTheDocument()

    it('400 requisicao_invalida lista os campos no topo do formulario', async () => {
      await falha(new ApiError(400, 'requisicao_invalida', 'Dados inválidos.', { campos: [{ campo: 'cep', mensagem: 'CEP inexistente.' }, { campo: 'inscricaoMunicipal', mensagem: 'Inválida.' }] }))
      await alerta('Corrija os dados: CEP: CEP inexistente.; inscrição municipal: Inválida.')
      expect(toastMock.success).not.toHaveBeenCalled()
    })
    it('409 documento_bloqueado', async () => {
      await falha(new ApiError(409, 'documento_bloqueado', 'x'))
      await alerta('O CNPJ não pode ser alterado depois do cadastro na IntegraNotas.')
    })
    it('409 documento_duplicado', async () => {
      await falha(new ApiError(409, 'documento_duplicado', 'x'))
      await alerta('Já existe uma empresa cadastrada com este CNPJ.')
    })
    it('403 cai na mensagem do back', async () => {
      await falha(new ApiError(403, 'acesso_negado', 'Sem permissão para isto.'))
      await alerta('Sem permissão para isto.')
    })
  })
})

describe('EmpresaConfig: certificado digital', () => {
  it('mostra o card, o texto de apoio e o estado "nao enviado"', async () => {
    prepara()
    renderTela()
    await razao()
    expect(screen.getByRole('heading', { name: 'Certificado digital e-CNPJ A1' })).toBeInTheDocument()
    expect(screen.getByText('Envie o certificado digital e-CNPJ A1 (arquivo .pfx). A senha deve ter só letras e números, sem acentos, até 60 caracteres.')).toBeInTheDocument()
    expect(screen.getByText('Certificado não enviado')).toBeInTheDocument()
    expect(campoArquivo()).toHaveAttribute('accept', '.pfx,.p12')
    expect(campoSenha()).toHaveAttribute('type', 'password')
    expect(enviarBtn()).toBeDisabled()
  })

  it('valido: badge e validade formatada, sem aviso de vencimento longe', async () => {
    prepara({ ...EMPRESA, certificado: { situacao: 'valido', validade: '2027-11-22', pendenteDeEnvio: false, venceEmDias: 365 } })
    renderTela()
    await razao()
    expect(screen.getByText('Certificado válido')).toBeInTheDocument()
    expect(screen.getByText('Válido até 22/11/2027')).toBeInTheDocument()
    expect(screen.queryByText(/vence em/)).not.toBeInTheDocument()
  })

  it.each([
    ['vencido', 'Certificado vencido'],
    ['invalido', 'Certificado inválido'],
    ['nao_pertence', 'Certificado de outro CNPJ'],
  ])('situacao %s aparece como tal', async (situacao, rotulo) => {
    prepara({ ...EMPRESA, certificado: { situacao, validade: '2026-01-10', pendenteDeEnvio: false, venceEmDias: -300 } })
    renderTela()
    await razao()
    expect(screen.getByText(rotulo)).toBeInTheDocument()
    expect(screen.queryByText(/O certificado vence/)).not.toBeInTheDocument()
  })

  it('vence em N dias (<= 30) mostra alerta; 31 nao; 1 dia no singular', async () => {
    prepara({ ...EMPRESA, certificado: { situacao: 'valido', validade: '2026-12-10', pendenteDeEnvio: false, venceEmDias: 18 } })
    renderTela()
    expect(await screen.findByText('O certificado vence em 18 dias.')).toBeInTheDocument()
  })
  it('31 dias nao alerta', async () => {
    prepara({ ...EMPRESA, certificado: { situacao: 'valido', validade: '2026-12-23', pendenteDeEnvio: false, venceEmDias: 31 } })
    renderTela()
    await razao()
    expect(screen.queryByText(/O certificado vence/)).not.toBeInTheDocument()
  })
  it('1 dia no singular', async () => {
    prepara({ ...EMPRESA, certificado: { situacao: 'valido', validade: '2026-11-23', pendenteDeEnvio: false, venceEmDias: 1 } })
    renderTela()
    expect(await screen.findByText('O certificado vence em 1 dia.')).toBeInTheDocument()
  })

  it('pendente de envio: badge e explicacao', async () => {
    prepara({ ...EMPRESA, certificado: { situacao: 'nao_enviado', validade: '2027-11-22', pendenteDeEnvio: true, venceEmDias: 365 } })
    renderTela()
    await razao()
    expect(screen.getByText('Aguardando habilitação da NFS-e')).toBeInTheDocument()
    expect(screen.getByText('O certificado está guardado e será enviado à IntegraNotas quando você habilitar a emissão em Configuração > NFS-e.')).toBeInTheDocument()
  })

  it('envia multipart com arquivo e senha, avisa, limpa o formulario e relê', async () => {
    prepara(EMPRESA, undefined, { situacao: 'valido', validade: '2027-11-22', pendenteDeEnvio: false, venceEmDias: 365, avisos: [] })
    const user = userEvent.setup()
    renderTela()
    await enviaCertificado(user)
    await waitFor(() => expect(posts()).toHaveLength(1))
    const [url, opts] = posts()[0]!
    expect(url).toBe(URL_C)
    expect(opts.method).toBe('POST')
    const corpo = opts.body as FormData
    expect(corpo).toBeInstanceOf(FormData)
    expect((corpo.get('arquivo') as File).name).toBe('empresa.pfx')
    expect(corpo.get('senha')).toBe('Senha123')
    await waitFor(() => expect(toastMock.success).toHaveBeenCalledWith('Certificado enviado.'))
    await waitFor(() => expect(gets().length).toBeGreaterThanOrEqual(2))
    expect(campoSenha()).toHaveValue('')
    expect(campoArquivo().files).toHaveLength(0)
    expect(enviarBtn()).toBeDisabled()
  })

  it('pendente: toast de certificado guardado', async () => {
    prepara(EMPRESA, undefined, { situacao: 'nao_enviado', validade: '2027-11-22', pendenteDeEnvio: true, venceEmDias: 365, avisos: [] })
    const user = userEvent.setup()
    renderTela()
    await enviaCertificado(user)
    await waitFor(() => expect(toastMock.success).toHaveBeenCalledWith('Certificado guardado. Será enviado quando a NFS-e for habilitada.'))
  })

  it('aviso conhecido vira Alert info; desconhecido e ignorado', async () => {
    prepara(EMPRESA, undefined, { situacao: 'valido', validade: '2027-11-22', pendenteDeEnvio: false, venceEmDias: 365, avisos: ['nao_foi_possivel_conferir_cnpj', 'outro_aviso'] })
    const user = userEvent.setup()
    renderTela()
    await enviaCertificado(user)
    expect(await screen.findByText('Não foi possível conferir o CNPJ do certificado.')).toBeInTheDocument()
    expect(screen.queryByText(/outro_aviso/)).not.toBeInTheDocument()
  })

  it('durante o envio: Enviando… e controles desabilitados', async () => {
    let solta: (v: unknown) => void = () => {}
    prepara(EMPRESA, undefined, () => new Promise((r) => (solta = r)))
    const user = userEvent.setup()
    renderTela()
    await enviaCertificado(user)
    expect(await screen.findByText('Enviando…')).toBeInTheDocument()
    expect(enviarBtn()).toBeDisabled()
    expect(campoArquivo()).toBeDisabled()
    expect(campoSenha()).toBeDisabled()
    solta({ ...NAO_ENVIADO, situacao: 'valido', avisos: [] })
    await waitFor(() => expect(screen.queryByText('Enviando…')).not.toBeInTheDocument())
  })

  describe('validacoes no cliente (nada e enviado)', () => {
    it('extensao diferente de .pfx/.p12', async () => {
      prepara()
      const user = userEvent.setup({ applyAccept: false })
      renderTela()
      await razao()
      await user.upload(campoArquivo(), pfx('certificado.txt'))
      expect(await screen.findByText('Envie um arquivo .pfx (ou .p12).')).toBeInTheDocument()
      await user.type(campoSenha(), 'Senha123')
      expect(enviarBtn()).toBeDisabled()
      expect(posts()).toHaveLength(0)
    })
    it('arquivo acima de 256 KB', async () => {
      prepara()
      const user = userEvent.setup()
      renderTela()
      await razao()
      await user.upload(campoArquivo(), pfx('grande.pfx', 256 * 1024 + 1))
      expect(await screen.findByText('Arquivo muito grande.')).toBeInTheDocument()
      expect(posts()).toHaveLength(0)
    })
    it('exatamente 256 KB e aceito', async () => {
      prepara()
      const user = userEvent.setup()
      renderTela()
      await razao()
      await user.upload(campoArquivo(), pfx('limite.pfx', 256 * 1024))
      await user.type(campoSenha(), 'Senha123')
      expect(screen.queryByText('Arquivo muito grande.')).not.toBeInTheDocument()
      expect(enviarBtn()).toBeEnabled()
    })
    it.each(['senha com espaco', 'Sênha1', 'a'.repeat(59) + 'ç'])('senha %j invalida', async (senha) => {
      prepara()
      const user = userEvent.setup()
      renderTela()
      await razao()
      await user.upload(campoArquivo(), pfx())
      await user.type(campoSenha(), senha)
      await user.click(enviarBtn())
      expect(await screen.findByText('A senha deve ter só letras e números, sem acentos.')).toBeInTheDocument()
      expect(posts()).toHaveLength(0)
    })
    it('botao desabilitado sem arquivo ou sem senha', async () => {
      prepara()
      const user = userEvent.setup()
      renderTela()
      await razao()
      await user.type(campoSenha(), 'Senha123')
      expect(enviarBtn()).toBeDisabled()
      await user.clear(campoSenha())
      await user.upload(campoArquivo(), pfx())
      expect(enviarBtn()).toBeDisabled()
    })
  })

  describe('erros do back ao enviar', () => {
    async function falha(e: ApiError) {
      prepara(EMPRESA, undefined, falhaCom(e))
      const user = userEvent.setup()
      renderTela()
      await enviaCertificado(user)
    }
    const alerta = async (texto: string) => expect(await screen.findByText(texto)).toBeInTheDocument()

    it.each([
      [400, 'certificado_senha_invalida', 'A senha deve ter só letras e números, sem acentos.'],
      [400, 'certificado_senha_incorreta', 'A senha do certificado está incorreta.'],
      [400, 'certificado_invalido', 'O arquivo não é um certificado válido (.pfx).'],
      [400, 'certificado_vencido', 'O certificado está vencido.'],
      [400, 'certificado_nao_pertence', 'O certificado pertence a outro CNPJ.'],
      [400, 'certificado_invalido_gateway', 'A IntegraNotas não conseguiu abrir o certificado (arquivo corrompido ou senha incorreta).'],
      [413, 'arquivo_grande', 'Arquivo muito grande.'],
      [429, 'muitas_tentativas', 'Muitas tentativas. Aguarde um pouco.'],
      [409, 'integranotas_token_recusado', 'A IntegraNotas recusou o token da integração. Fale com o suporte.'],
      [504, 'integranotas_indisponivel', 'A IntegraNotas não respondeu a tempo. Tente de novo.'],
      [502, 'integranotas_resposta_invalida', 'A IntegraNotas devolveu uma resposta inesperada. Tente de novo.'],
    ])('%i %s', async (status, codigo, texto) => {
      await falha(new ApiError(status, codigo, 'mensagem do back'))
      await alerta(texto)
      expect(toastMock.success).not.toHaveBeenCalled()
    })

    it('codigo desconhecido cai na mensagem do back', async () => {
      await falha(new ApiError(403, 'acesso_negado', 'Sem permissão para isto.'))
      await alerta('Sem permissão para isto.')
    })

    it('erro nao apaga o arquivo escolhido, mas a senha ja foi descartada', async () => {
      await falha(new ApiError(400, 'certificado_senha_incorreta', 'x'))
      await alerta('A senha do certificado está incorreta.')
      expect(campoSenha()).toHaveValue('')
      expect(campoArquivo().files?.[0]?.name).toBe('empresa.pfx')
      expect(enviarBtn()).toBeDisabled()
    })
  })

  describe('a senha nunca fica no estado', () => {
    it('a mutation zera a senha das variaveis ao montar o corpo', async () => {
      prepara()
      const qc = new QueryClient({ defaultOptions: { queries: { retry: false } } })
      const wrapper = ({ children }: { children: ReactNode }) => <QueryClientProvider client={qc}>{children}</QueryClientProvider>
      const { result } = renderHook(() => useEnviarCertificado(), { wrapper })
      result.current.mutate({ arquivo: pfx(), senha: 'Segredo99' })
      await waitFor(() => expect(result.current.isSuccess).toBe(true))
      expect(result.current.variables?.senha).toBe('')
      expect((posts()[0]![1].body as FormData).get('senha')).toBe('Segredo99')
    })

    it('nada de senha no DOM depois do envio com sucesso', async () => {
      prepara()
      const user = userEvent.setup()
      renderTela()
      await enviaCertificado(user, 'empresa.pfx', 'Segredo99')
      await waitFor(() => expect(toastMock.success).toHaveBeenCalled())
      expect(campoSenha()).toHaveValue('')
      expect(document.body.innerHTML).not.toContain('Segredo99')
    })
  })
})
