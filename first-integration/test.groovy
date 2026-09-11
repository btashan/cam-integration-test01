 def isim = request.headers.get("isim")
                def dogumTarihiStr = request.headers.get("dogumTarihi")

                if (!isim || !dogumTarihiStr) {
                    return [
                        status: "ERROR",
                        message: "Lutfen 'isim' ve 'dogumTarihi' (YYYY-MM-DD) parametrelerini girin."
                    ] 
                }

                def dogumTarihi = java.time.LocalDate.parse(dogumTarihiStr)
                def yas = java.time.Period.between(dogumTarihi, java.time.LocalDate.now()).years
                
                return [
                    isim: isim,
                    yas: yas,
                    mesaj: "Merhaba " + isim + ", " + yas + " yasindasiniz."
                ]
