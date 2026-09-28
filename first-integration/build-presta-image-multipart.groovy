/*
 * Data transformation: Odoo'nun base64 image_256 alanini PrestaShop
 * /api/images/products/{id} endpoint'inin bekledigi multipart/form-data
 * ("image" alani) govdesine cevirir. PrestaShop dosya adi uzantisi ve
 * mime type'i dogru degilse "Image format not recognized" (400) donuyor, bu
 * yuzden format magic byte'lardan tespit ediliyor. Desteklenmeyen formatta
 * (ornegin SVG) prestaImageSupported=false set edilir, XML tarafi atlar.
 */

def p = exchange.getProperty('odooProduct')
byte[] data = p.image_256.decodeBase64()

def format = null
if (data.length > 3 && (data[0] & 0xff) == 0xff && (data[1] & 0xff) == 0xd8) {
    format = ['jpg', 'image/jpeg']
} else if (data.length > 8 && (data[0] & 0xff) == 0x89 && new String(data, 1, 3, 'US-ASCII') == 'PNG') {
    format = ['png', 'image/png']
} else if (data.length > 6 && new String(data, 0, 3, 'US-ASCII') == 'GIF') {
    format = ['gif', 'image/gif']
} else if (data.length > 12 && new String(data, 0, 4, 'US-ASCII') == 'RIFF' && new String(data, 8, 4, 'US-ASCII') == 'WEBP') {
    format = ['webp', 'image/webp']
}

exchange.setProperty('prestaImageSupported', format != null)
if (format == null) {
    return
}

def boundary = "----camelyne-${UUID.randomUUID()}"
def out = new ByteArrayOutputStream()
out << ("--${boundary}\r\n"
        + "Content-Disposition: form-data; name=\"image\"; filename=\"odoo-${p.id}.${format[0]}\"\r\n"
        + "Content-Type: ${format[1]}\r\n\r\n").getBytes('US-ASCII')
out << data
out << "\r\n--${boundary}--\r\n".getBytes('US-ASCII')

exchange.message.setHeader('Content-Type', "multipart/form-data; boundary=${boundary}")
exchange.message.body = out.toByteArray()
