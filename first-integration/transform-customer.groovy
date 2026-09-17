/*
 * Data transformation: split'ten gelen tek bir PrestaShop <customer> XML
 * node'unu, downstream route'larin (Odoo cagrilari) kullanacagi duz bir
 * Map'e cevirir. Orkestrasyon (HTTP cagrilari, create/write karari, state
 * okuma/yazma) Camel XML DSL tarafinda; burada sadece alan esleme var.
 */

def node = exchange.message.body

def text = { String tag ->
    def list = node.getElementsByTagName(tag)
    (list != null && list.length > 0) ? list.item(0).textContent : ''
}

exchange.message.body = [
        id       : text('id'),
        firstname: text('firstname'),
        lastname : text('lastname'),
        email    : text('email'),
        dateUpd  : text('date_upd'),
]
