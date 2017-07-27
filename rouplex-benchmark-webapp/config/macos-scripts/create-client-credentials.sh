#!/usr/bin/env bash
echo "Rouplex --- Creating client credentials"

if [ -z "$1" ]
then
    echo "Please supply the client name as first parameter"
    exit 1
fi
client_name=$1
client_folder=client-credentials/$client_name

if [ -z "$2" ]
then
    echo "Please supply the organization name of the client as second parameter (sub-ca which is going to sign the client's credentials)"
    exit 1
fi
organization_name=$2
client_path=$client_folder/$client_name-$organization_name

echo "Rouplex --- Creating directory structure"
mkdir client-credentials
mkdir $client_folder
pushd $client_folder

echo "Rouplex --- Creating private key for $client_name"
openssl genrsa -aes128 -passout pass:$client_name -out $client_name.key 2048

echo "[req]
prompt = no
distinguished_name = dn
input_password = $client_name

[dn]
CN = $client_name
O = $organization_name
C = US" > client.conf

echo "Rouplex --- Creating certificate signing request for $client_name"
openssl req -new -config client.conf -key $client_name.key -out $client_name-$organization_name.csr

popd

echo "Rouplex --- Signing certificate by sub-ca-$organization_name"
echo "Rouplex === [The password you are asked for, is of the sub-ca] ==="
pushd sub-cas/sub-ca-$organization_name
./sign-client-csr.sh ../../$client_path.csr ../../$client_path.crt
popd

echo "Rouplex --- Deleting the certificate signing request since it is not needed anymore"
rm $client_path.csr

echo "Rouplex --- Creating pkcs12 certificate (private key and certificate chain included) ready for use on ssl client"
echo "Rouplex === [The password you are asked for, is for protecting the client-keystore.p12 and you can chose '$client_name' (no quotes) for the sake of this demo] ==="
cat $client_path.crt > $client_path-cert-chain.crt
cat sub-cas/sub-ca-$organization_name/$organization_name.crt >> $client_path-cert-chain.crt
cat root-ca/root-ca.crt >> $client_path-cert-chain.crt
openssl pkcs12 -export -name "$client_name" -out $client_path.p12 -inkey $client_folder/$client_name.key -in $client_path.crt -certfile $client_path-cert-chain.crt

echo "Rouplex --- Created client credentials"
