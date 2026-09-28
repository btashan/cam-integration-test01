import groovy.json.JsonSlurper
import groovy.json.JsonOutput

/*
 * Data transformation: NewStore Order Management API'sinden gelen tek bir
 * sales order JSON govdesini, downstream route'larin kullanacagi duz bir
 * Map/JSON'a cevirir.
 *
 * NewStore, PrestaShop'un aksine order verisini XML degil dogrudan JSON
 * olarak dondurur (bkz. Sales Order API: GET /v0/d/orders/{id} veya
 * /v0/d/orders?...). Bu yuzden burada DOM/XmlSlurper degil JsonSlurper
 * kullaniliyor.
 *
 * NOT: Asagidaki alan adlari NewStore'un genel olarak dokumante edilen
 * sales order semasina dayanir. Eger senin tenant/versiyonunda farkli
 * alan adlari varsa (ornegin customer_order_id yerine consumer_order_id
 * gibi) lutfen orijinal JSON ornegini paylas, ona gore duzeltelim.
 */

def bodyIn = exchange.message.body

// body String, byte[] veya InputStream olabilir - hepsini JsonSlurper
// karsilayabiliyor; yine de guvenli tarafta kalmak icin text'e ceviriyoruz.
def rawText = (bodyIn instanceof String) ? bodyIn : bodyIn.toString()
def order = new JsonSlurper().parseText(rawText)

def val = { field -> order?.get(field) }

def address = { addr ->
    if (addr == null) return null
    [
            first_name     : addr.first_name,
            last_name      : addr.last_name,
            address_line_1 : addr.address_line_1,
            address_line_2 : addr.address_line_2,
            city            : addr.city,
            state           : addr.state,
            zip_code        : addr.zip_code,
            country         : addr.country,
            phone           : addr.phone,
    ]
}

def items = (order?.items ?: []).collect { item ->
    [
            id                    : item.id,
            product_id            : item.product_id,
            sku                   : item.sku ?: item.uid,
            quantity              : item.quantity,
            price                 : item.price,
            price_after_discounts : item.price_after_discounts,
            item_status           : item.item_status,
            fulfillment_location_id: item.fulfillment_location_id,
            shipment_id           : item.shipment_id,
            tax_lines             : (item.tax_lines ?: []).collect { tax ->
                [name: tax.name, rate: tax.rate, amount: tax.amount]
            },
            discounts             : (item.discounts ?: []).collect { d ->
                [reason: d.reason, amount: d.amount]
            },
    ]
}

def payments = (order?.payments ?: []).collect { p ->
    [
            id      : p.id,
            type    : p.type,
            amount  : p.amount,
            currency: p.currency,
            status  : p.status,
    ]
}

def shipments = (order?.shipments ?: []).collect { s ->
    [
            id             : s.id,
            status         : s.status,
            carrier        : s.carrier,
            tracking_number: s.tracking_number,
            shipped_at     : s.shipped_at,
    ]
}

def result = [
        id                  : val('id'),
        order_id            : val('order_id'),
        external_id         : val('external_id'),
        customer_order_id   : val('customer_order_id') ?: val('consumer_order_id'),
        tenant              : val('tenant'),
        channel_type        : val('channel_type'),
        channel_name        : val('channel_name'),
        currency            : val('currency'),
        is_exchange         : val('is_exchange'),
        is_historical       : val('is_historical'),
        financial_status    : val('financial_status'),
        fulfillment_status  : val('fulfillment_status'),
        created_at          : val('created_at'),
        updated_at          : val('updated_at'),
        customer_id         : order?.customer?.id,
        customer_email      : order?.customer?.email,
        customer_first_name : order?.customer?.first_name,
        customer_last_name  : order?.customer?.last_name,
        shipping_address    : address(order?.shipping_address),
        billing_address     : address(order?.billing_address),
        items               : items,
        payments            : payments,
        shipments           : shipments,
        extended_attributes : val('extended_attributes'),
]

exchange.message.body = JsonOutput.toJson(result)
exchange.message.setHeader('Content-Type', 'application/json')
