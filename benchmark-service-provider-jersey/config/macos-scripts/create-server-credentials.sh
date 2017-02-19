#!/usr/bin/env bash
echo "Rouplex --- Creating server credentials"

if [ -z "$1" ]
then
    echo "Please supply the domain name for the server certificate"
    exit 1
fi
domain_name=$1
server_folder=server-credentials/$domain_name

if [ -z "$2" ]
then
    echo "Please supply the organization name of the server (sub-ca which is going to sign the server's credentials)"
    exit 1
fi
organization_name=$2
server_path=$server_folder/$domain_name-$organization_name

sub_ca_folder=sub-cas/sub-ca-$organization_name
if [ ! -d $sub_ca_folder ]
then
	echo "$sub_ca_folder does not exist. You must create the sub ca first (you can use create-sub-ca.sh script for that)"
	exit 1
fi

echo "Rouplex --- Creating directory structure"
mkdir server-credentials
mkdir $server_folder
pushd $server_folder

echo "Rouplex --- Creating private key for $domain_name"
openssl genrsa -aes128 -passout pass:$domain_name -out $domain_name.key 2048

echo "[req]
prompt = no
distinguished_name = dn
req_extensions = ext
input_password = $domain_name

[dn]
CN = $domain_name
O = $organization_name
C = US

[ext]
subjectAltName = DNS:www.$domain_name,DNS:$domain_name" > server.conf

echo "Rouplex --- Creating certificate signing request for $domain_name"
openssl req -new -config server.conf -key $domain_name.key -out $domain_name-$organization_name.csr

popd

echo "Rouplex --- Signing certificate signing request by sub-ca-$organization_name"
echo "Rouplex === [The password you are asked for, is of the sub-ca] ==="
pushd $sub_ca_folder
./sign-server-csr.sh ../../$server_path.csr ../../$server_path.crt
popd

echo "Rouplex --- Deleting the csr since it is not needed anymore"
rm $server_path.csr

echo "Rouplex --- Creating pkcs12 certificate (private key and certificate chain included) ready for use on ssl server"
echo "Rouplex === [The password you are asked for, is for protecting the server-keystore.p12 and you can chose '$domain_name' (no quotes) for the sake of this demo] ==="
cat $server_path.crt > $server_path-cert-chain.crt
cat sub-cas/sub-ca-$organization_name/$organization_name.crt >> $server_path-cert-chain.crt
cat root-ca/root-ca.crt >> $server_path-cert-chain.crt
openssl pkcs12 -export -name "$domain_name" -out $server_path.p12 -inkey $server_folder/$domain_name.key -in $server_path.crt -certfile $server_path-cert-chain.crt

echo "Rouplex --- Created server credentials"
