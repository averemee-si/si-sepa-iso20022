# si-sepa-iso20022

pain.001.001.03 &amp; camt.053.001.02 to [Revolut](https://business.revolut.com/)  conversion utility

For GUI (Unix/Linux/macOS only):

```
java -jar si-sepa-iso20022-1.0.0-standalone.jar
```

For CLI (any OS with Java 11+):

```
java -cp si-sepa-iso20022-1.0.0-standalone.jar solutions.a2.iso20022.si.revolut.BankToCustomerStatement \
    -n "A2 Rešitve digitalne storitve d.o.o." \
    -i LT0123456789012345678 \
    -a "Glavni Trg 4 Maribor 2000" \
    -s statement-2022-05.csv

java -cp si-sepa-iso20022-1.0.0-standalone.jar solutions.a2.iso20022.si.revolut.CreditTransferInitiation \
    -s vacation-2022-TkDis.xml \
    -i "SI0123456789012345678,DE0123456789012345678"

```
