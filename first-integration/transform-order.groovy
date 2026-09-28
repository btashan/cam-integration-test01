import groovy.json.JsonOutput
import groovy.xml.XmlSlurper
import javax.xml.transform.TransformerFactory
import javax.xml.transform.dom.DOMSource
import javax.xml.transform.stream.StreamResult

def node = exchange.message.body

// split, body'e bir org.w3c.dom.Node veriyor; XmlSlurper string bekledigi
// icin once bunu XML metnine seri hale getiriyoruz.
def writer = new StringWriter()
TransformerFactory.newInstance().newTransformer().transform(new DOMSource(node), new StreamResult(writer))
def order = new XmlSlurper().parseText(writer.toString())

// GPath'te olmayan bir alan bos NodeChildren dondurur, .text() de '' verir;
// yine de null-safe olmasi icin kucuk bir yardimci kullaniyoruz.
def val = { field -> order[field].text() }

def orderRows = order.associations.order_rows.order_row.collect { row ->
    [
            id                  : row.id.text(),
            product_id          : row.product_id.text(),
            product_attribute_id: row.product_attribute_id.text(),
            product_quantity    : row.product_quantity.text(),
            product_name        : row.product_name.text(),
            product_reference   : row.product_reference.text(),
            product_ean13       : row.product_ean13.text(),
            product_isbn        : row.product_isbn.text(),
            product_upc         : row.product_upc.text(),
            product_price       : row.product_price.text(),
            id_customization    : row.id_customization.text(),
            unit_price_tax_incl : row.unit_price_tax_incl.text(),
            unit_price_tax_excl : row.unit_price_tax_excl.text(),
    ]
}

def result = [
        id                        : val('id'),
        id_address_delivery       : val('id_address_delivery'),
        id_address_invoice        : val('id_address_invoice'),
        id_cart                   : val('id_cart'),
        id_currency               : val('id_currency'),
        id_lang                   : val('id_lang'),
        id_customer               : val('id_customer'),
        id_carrier                : val('id_carrier'),
        current_state             : val('current_state'),
        module                    : val('module'),
        invoice_number            : val('invoice_number'),
        invoice_date              : val('invoice_date'),
        delivery_number           : val('delivery_number'),
        delivery_date             : val('delivery_date'),
        valid                     : val('valid'),
        date_add                  : val('date_add'),
        date_upd                  : val('date_upd'),
        shipping_number           : val('shipping_number'),
        note                      : val('note'),
        id_shop_group             : val('id_shop_group'),
        id_shop                   : val('id_shop'),
        secure_key                : val('secure_key'),
        payment                   : val('payment'),
        recyclable                : val('recyclable'),
        gift                      : val('gift'),
        gift_message              : val('gift_message'),
        mobile_theme              : val('mobile_theme'),
        total_discounts           : val('total_discounts'),
        total_discounts_tax_incl  : val('total_discounts_tax_incl'),
        total_discounts_tax_excl  : val('total_discounts_tax_excl'),
        total_paid                : val('total_paid'),
        total_paid_tax_incl       : val('total_paid_tax_incl'),
        total_paid_tax_excl       : val('total_paid_tax_excl'),
        total_paid_real           : val('total_paid_real'),
        total_products            : val('total_products'),
        total_products_wt         : val('total_products_wt'),
        total_shipping            : val('total_shipping'),
        total_shipping_tax_incl   : val('total_shipping_tax_incl'),
        total_shipping_tax_excl   : val('total_shipping_tax_excl'),
        carrier_tax_rate          : val('carrier_tax_rate'),
        total_wrapping            : val('total_wrapping'),
        total_wrapping_tax_incl   : val('total_wrapping_tax_incl'),
        total_wrapping_tax_excl   : val('total_wrapping_tax_excl'),
        round_mode                : val('round_mode'),
        round_type                : val('round_type'),
        conversion_rate           : val('conversion_rate'),
        reference                 : val('reference'),
        order_rows                : orderRows,
]

exchange.message.body = JsonOutput.toJson(result)
exchange.message.setHeader('Content-Type', 'application/json')
