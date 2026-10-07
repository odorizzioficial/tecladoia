package com.odorizzioficial.tecladoia.service

/**
 * Apps em que a barra fica desligada: nada e desenhado sobre a tela e nenhum
 * campo e lido. Bancos e carteiras digitais se protegem de overlays e de
 * servicos de acessibilidade, e o certo e o TecladoIA nem encostar neles.
 *
 * A lista cobre os mais comuns; o usuario completa a dela em
 * Ajustes > Apps de banco e pagamento.
 */
object SensitiveApps {

    /** Inicios de nome de pacote de bancos, carteiras e corretoras conhecidos. */
    private val KNOWN_PREFIXES = listOf(
        // Brasil
        "com.nu.production", "com.nubank",
        "com.itau", "br.com.itau",
        "com.bradesco", "br.com.bradesco",
        "com.santander", "br.com.santander",
        "br.com.bb.", "br.gov.caixa", "br.com.gabba.caixa", "com.caixa",
        "br.com.intermedium", "com.c6bank", "br.com.c6",
        "com.picpay", "br.com.picpay", "com.mercadopago",
        "br.com.uol.ps", "br.com.pagseguro", "br.com.neon", "br.com.original",
        "br.com.sicredi", "coop.sicredi", "br.com.sicoob", "br.coop.sicoob",
        "br.com.banrisul", "com.banrisul",
        "br.com.btgpactual", "com.btg", "br.com.xp.",
        "br.com.safra", "com.safra", "br.com.willbank", "com.willbank",
        "br.com.stone", "br.com.cielo", "br.com.getnet",
        // Internacional
        "com.paypal", "com.venmo", "com.squareup.cash",
        "com.coinbase", "com.binance", "io.metamask",
        "com.revolut", "com.transferwise",
        "com.chase", "com.infonow.bofa", "com.wf.wellsfargomobile", "com.citi",
        "com.americanexpress",
        "com.google.android.apps.walletnfcrel", "com.google.android.apps.nbu.paisa",
        "com.samsung.android.spay", "com.samsung.android.spaylite"
    )

    /** Pedacos que quase sempre indicam um app bancario. */
    private val KEYWORDS = listOf("bank", "banco", "nubank")

    fun isFinancial(packageName: String): Boolean {
        val pkg = packageName.lowercase()
        return KNOWN_PREFIXES.any { pkg.startsWith(it) } || KEYWORDS.any { pkg.contains(it) }
    }

    /** O app em primeiro plano deve ser deixado em paz? */
    fun isProtected(
        packageName: String?,
        protectFinancial: Boolean,
        custom: Set<String>
    ): Boolean {
        if (packageName.isNullOrEmpty()) return false
        if (packageName in custom) return true
        return protectFinancial && isFinancial(packageName)
    }
}
