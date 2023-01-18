echo "-----------------------------------------------------------------------------------"
echo "########### Setting env for creating keystore ###########"
export KEY_STORE=zufarkeystore     #Specifies the keystore name
export STORE_TYPE=JKS              #Specifies the keystore type
export ALIAS=zufarkeystore         #Specifies the alias name of the entry to process
export KEY_ALGORITHM=RSA           #Specifies the algorithm to be used to generate the key pair
export SIG_ALGORITHM=SHA256withRSA #Specifies the algorithm that should be used to sign the self-signed certificate
export KEY_SIZE=4096               #Specifies the size of each key to be generated
export VALIDITY=365                #Specifies the validity of the keystore that you want to create
export STORE_PASS=newpass          #Specifies the keystore password
export KEY_TOOL_PATH="C:\Program Files\BellSoft\LibericaJDK-17\bin" #Specifies the keytool path

echo "KEY_STORE          = ${KEY_STORE}"
echo "STORE_TYPE         = ${STORE_TYPE}"
echo "ALIAS              = ${ALIAS}"
echo "KEY_ALGORITHM      = ${KEY_ALGORITHM}"
echo "SIG_ALGORITHM      = ${SIG_ALGORITHM}"
echo "KEY_SIZE           = ${KEY_SIZE}"
echo "VALIDITY           = ${VALIDITY}"
echo "STORE_PASS         = ${STORE_PASS}"
echo "KEY_TOOL_PATH      = ${KEY_TOOL_PATH}"

echo "-----------------------------------------------------------------------------------"
echo "########### Setting env for certificate ###########"

export COMMON_NAME="Zufar Sunagatov"                #Specifies the common name
export EMAIL_ADDRESS="zufar.sunagatov@gmail.com"    #Specifies the email address
export ORGANIZATION_UNIT="Zufar Organization Unit"  #Specifies the organization unit
export ORGANIZATION="Zufar Organization"            #Specifies the organization
export LOCALITY="London"                            #Specifies the locality
export STATE="London State"                         #Specifies the state
export COUNTRY="GB"                                 #Specifies the country code
export DOMAIN_COMPONENT="com"                       #Specifies the domain component name

echo "COMMON_NAME          = ${COMMON_NAME}"
echo "EMAIL_ADDRESS        = ${EMAIL_ADDRESS}"
echo "ORGANIZATION_UNIT    = ${ORGANIZATION_UNIT}"
echo "ORGANIZATION         = ${ORGANIZATION}"
echo "LOCALITY             = ${LOCALITY}"
echo "STATE                = ${STATE}"
echo "COUNTRY              = ${COUNTRY}"
echo "DOMAIN_COMPONENT     = ${DOMAIN_COMPONENT}"

echo "-----------------------------------------------------------------------------------"
echo "########### Generating ${KEY_SIZE} bit ${KEY_ALGORITHM} key pair and self-signed certificate (${SIG_ALGORITHM}) with a validity of ${VALIDITY} days ###########"
"${KEY_TOOL_PATH}\keytool" -keystore $KEY_STORE \
 -genkeypair \
 -alias $ALIAS \
 -keyalg $KEY_ALGORITHM \
 -sigalg $SIG_ALGORITHM \
 -keysize $KEY_SIZE \
 -storetype $STORE_TYPE \
 -validity $VALIDITY \
 -storepass $STORE_PASS \
 -dname "CN=${COMMON_NAME}, OU=${ORGANIZATION_UNIT}, O=${ORGANIZATION}, L=${LOCALITY}, ST=${STATE}, C=${COUNTRY}, DC=${DOMAIN_COMPONENT}"
echo "-----------------------------------------------------------------------------------"