/*
 * Data transformation: staging dosyasinin mevcut icerigi (body: urun Map
 * listesi, ilk calistirmada bos liste) ile bu tick'te Odoo'dan gelen urunleri
 * (fetchedProducts property'si) id bazinda birlestirir. Ayni urun tekrar
 * gelirse en son hali kalir ve listenin sonuna tasinir (write_date sirasi).
 * Dosya okuma/yazma ve JSON (un)marshal Camel XML DSL tarafinda; burada sadece
 * birlestirme var.
 */

def byId = new LinkedHashMap()
exchange.message.body.each { product -> byId[product.id] = product }
exchange.getProperty('fetchedProducts').each { product ->
    byId.remove(product.id)
    byId[product.id] = product
}

exchange.setProperty('stagingSize', byId.size())
exchange.message.body = new ArrayList(byId.values())
