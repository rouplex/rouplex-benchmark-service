#!/usr/bin/env bash

echo "Rouplex --- Creating root-ca"
root_ca_folder=root-ca
if [ -d $root_ca_folder ]
then
	echo "Rouplex --- $root_ca_folder folder already exists. You must delete the existing folder first in order to create a new root ca"
	exit 1
fi

echo "Rouplex --- Creating directory structure and copying templates at `pwd`/$root_ca_folder folder"
mkdir $root_ca_folder
cp -r templates/root-ca/* $root_ca_folder

pushd $root_ca_folder
mkdir certs db private
chmod 700 private
touch db/index
# openssl rand -hex 16  > db/serial
echo AA14 > db/serial
echo 1001 > db/crlnumber

echo "Rouplex --- Creating private key and certificate signing request"
echo "Rouplex === [The password you are asked for twice, is of the root-ca] ==="
openssl req -new -config root-ca.conf -out root-ca.csr -keyout private/root-ca.key

echo "Rouplex --- Self signing the certificate signing request"
echo "Rouplex === [The password you are asked for, is of the root-ca] ==="
openssl ca -selfsign -config root-ca.conf -in root-ca.csr -out root-ca.crt -extensions ca_ext

echo "Rouplex --- Deleting the certificate signing request since it is not needed anymore"
rm root-ca.csr

popd
echo "Rouplex --- Created root-ca"
