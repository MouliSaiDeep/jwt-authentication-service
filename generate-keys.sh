#!/bin/bash
set -e

# Create keys directory if it doesn't exist
mkdir -p keys

# Set restrictive permissions early
chmod 700 keys

# Generate RSA-2048 private key
openssl genpkey -algorithm RSA -out keys/private.pem -pkeyopt rsa_keygen_bits:2048

# Extract public key from private key
openssl rsa -in keys/private.pem -pubout -out keys/public.pem

# Restrict permissions on keys
chmod 600 keys/private.pem
chmod 644 keys/public.pem

echo "RSA key pair generated successfully in the 'keys/' directory."
