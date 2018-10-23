# Signing ceremony

Once the wallet has funds, a signing ceremony can take place.

A quorum of HSMs are provided metadata about the transaction being signed. One signature per input is returned to
the Coordinator service.

<!--
Admin -> Coordinator Service: initiate signing ceremony
Coordinator Service -> Operator: SignTxRequest (QR code displayed)
Operator -> Java UI: SignTxRequest (QR code scanned)
note over Java UI
confirm signing operation
prompt for operator smart card
end note
Java UI -> HSM Core: SignTxRequest (Socket)
HSM Core -> Java UI: signatures
Java UI -> Operator: SignTxResponse (QR code displayed)
Operator -> Coordinator Service: SignTxResponse (QR code scanned)
note over Coordinator Service
signatures are merged and the
transaction is broadcasted
end note
-->

<img src="../signing_ceremony.png">
