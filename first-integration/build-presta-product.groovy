/*
 * Data transformation: tek bir Odoo product.product kaydini (odooProduct
 * property'si) PrestaShop webservice <product> XML'ine cevirir.
 *   - Yeni urun (prestaProductId yok): POST icin tam kayit (kategori, vergi
 *     grubu, urun tipi, link_rewrite, mpn eslestirme anahtari dahil).
 *   - Mevcut urun: PATCH icin sadece Odoo'nun sahip oldugu alanlar (isim,
 *     fiyat, referans, barkod, aciklama, aktiflik). Kategori/SEO gibi
 *     PrestaShop'ta sonradan duzenlenen alanlar ezilmez.
 * HTTP cagrisi ve create/PATCH karari Camel XML DSL tarafinda.
 */

def p = exchange.getProperty('odooProduct')
def prestaId = exchange.getProperty('prestaProductId')
def langIds = exchange.getProperty('prestaLanguageIds').toString().split(',')*.trim()
def categoryId = exchange.getProperty('prestaCategoryId')

// Odoo bos alanlari null degil false olarak doner.
def text = { v -> (v == null || v == false) ? '' : v.toString() }
def esc = { String v -> v.replace('&', '&amp;').replace('<', '&lt;').replace('>', '&gt;').replace('"', '&quot;') }
def multiLang = { String tag, String value ->
    "<${tag}>" + langIds.collect { "<language id=\"${it}\">${esc(value)}</language>" }.join('') + "</${tag}>"
}

// display_name "[KOD] Isim" formatinda, bu yuzden once name. PrestaShop
// isCatalogName: <>;=#{} yasak, max 128 karakter.
def name = text(p.name ?: p.display_name).replaceAll('[<>;=#{}]', ' ').trim().take(128)
if (!name) {
    name = "Odoo product ${p.id}"
}
def reference = text(p.default_code).replaceAll('[<>;={}]', ' ').trim().take(64)
// Gecersiz barkod PrestaShop'ta 400 (isEan13) dondurur: sadece 13 haneye kadar rakamsa gonder.
def barcode = text(p.barcode)
def ean13 = barcode ==~ /\d{1,13}/ ? barcode : ''
// Double.toString buyuk sayilarda 1.0E7 uretir, PrestaShop isPrice kabul etmez.
def price = new BigDecimal((p.lst_price ?: 0).toString()).toPlainString()
def isService = p.type == 'service'

def xml = new StringBuilder('<?xml version="1.0" encoding="UTF-8"?><prestashop><product>')
if (prestaId) {
    xml << "<id>${prestaId}</id>"
} else {
    def slug = name.toLowerCase().replaceAll('[^a-z0-9]+', '-').replaceAll('^-+|-+$', '')
    xml << "<mpn>${esc(exchange.getProperty('prestaMpn'))}</mpn>"
    xml << "<id_category_default>${categoryId}</id_category_default>"
    xml << "<id_tax_rules_group>${exchange.getProperty('prestaTaxRulesGroupId')}</id_tax_rules_group>"
    xml << "<product_type>${isService ? 'virtual' : 'standard'}</product_type>"
    xml << "<is_virtual>${isService ? 1 : 0}</is_virtual>"
    // state=1 olmazsa PrestaShop urunu taslak (gecici) olarak birakir.
    xml << '<state>1</state><visibility>both</visibility><available_for_order>1</available_for_order>'
    xml << '<show_price>1</show_price><minimal_quantity>1</minimal_quantity>'
    xml << multiLang('link_rewrite', slug ?: "odoo-product-${p.id}")
}
xml << "<reference>${esc(reference)}</reference>"
if (ean13) {
    xml << "<ean13>${ean13}</ean13>"
}
xml << "<price>${price}</price>"
xml << "<active>${p.active ? 1 : 0}</active>"
xml << multiLang('name', name)
xml << multiLang('description', text(p.description))
if (!prestaId) {
    xml << "<associations><categories><category><id>${categoryId}</id></category></categories></associations>"
}
xml << '</product></prestashop>'

exchange.message.body = xml.toString()
