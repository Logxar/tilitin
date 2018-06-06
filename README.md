# Process

## Import model

Tosite, Päivämäärä, Nro,    Tili,           Debet,  Kredit, Selite
1,      5.2.2018,   4100,   Alkoholijuomat, 15.00,  ,       Juttu
1,      5.2.2018,   1910,   Pankkitili 1,   ,       15.00,  Toinen juttu

## Copy model

Nro ,   Tili          , Debet,  Kredit, VAT,    Selite
4100,   Alkoholijuomat, 15.00,        ,    ,    Juttu
1910,   Pankkitili 1  ,      ,  15.00 ,    ,    Toinen juttu

## Create new entry

1. Uusi tosite
    - DocumentFrame.createDocument()
2. Päivitä päivämäärä
    - (dateTextField.setDate(document.getDate()))
    - dateTextField.setText(dateStr)
3. Copy info to clipboard
    -
<!-- 3. Lisää vienti
    - DocumentFrame.addEntry()
4. Valitse tili
    - DocumentFrame.showAccountSelection(String q)
    - DocumentFrame.accountSelected() -->
