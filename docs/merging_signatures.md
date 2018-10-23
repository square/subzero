# Merging signatures

Once the Coordinator receives all the signatures, it can merge them and broadcast the transaction.

The signatures need to be appended in a specific order (same order as their corresponding public keys). The code in
`ui/shared` can be used to perform this operation if the Coordinator is implemented in Java.
