package it.atlantica.AlgoDemo.Controller;

import com.algorand.algosdk.account.Account;
import com.algorand.algosdk.crypto.Address;
import com.algorand.algosdk.mnemonic.Mnemonic;
import com.algorand.algosdk.transaction.SignedTransaction;
import com.algorand.algosdk.transaction.Transaction;
import com.algorand.algosdk.util.CryptoProvider;
import com.algorand.algosdk.util.Encoder;
import com.algorand.algosdk.v2.client.Utils;
import com.algorand.algosdk.v2.client.common.AlgodClient;
import com.algorand.algosdk.v2.client.common.IndexerClient;
import com.algorand.algosdk.v2.client.common.Response;
import com.algorand.algosdk.v2.client.model.Asset;
import com.algorand.algosdk.v2.client.model.PendingTransactionResponse;
import com.algorand.algosdk.v2.client.model.PostTransactionsResponse;
import com.algorand.algosdk.v2.client.model.TransactionParametersResponse;

import it.atlantica.AlgoDemo.Model.DataTrans;


import org.json.JSONArray;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigInteger;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.KeyPair;
import java.security.NoSuchAlgorithmException;
import java.security.PublicKey;
import java.util.Arrays;
import java.util.Base64;
import java.util.Date;
import java.util.List;




@RestController
public class CreateWallet {

	
	 private AlgodClient client = connectToNetwork(); 
     // utility function to connect to a node
	 private AlgodClient connectToNetwork() {
       final String ALGOD_API_ADDR = "localhost";
       final String ALGOD_API_TOKEN = "aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa";
       final Integer ALGOD_PORT = 4001;

       AlgodClient client = new AlgodClient(ALGOD_API_ADDR,
           ALGOD_PORT, ALGOD_API_TOKEN);
       return client;
   }
    /**
     * A method to retrieve account details as specified in the properties file
     * @return String representing account data
     * @throws Exception
     */
    @GetMapping(path="/account", produces = "application/json; charset=UTF-8")
    public String createAccount() throws Exception {
        try {
            Account account = new Account();
            System.out.println("My Address: " + account.getAddress());
            System.out.println("My Passphrase: " + account.toMnemonic());
            System.out.println("Navigate to this link and dispense funds:  https://dispenser.testnet.aws.algodev.network?account=" + account.getAddress().toString());            
            return account.toMnemonic().toString();
            // Copy off account and mnemonic
            // Dispense TestNet Algos to account:
            // https://dispenser.testnet.aws.algodev.network/
            // resource:
            // https://developer.algorand.org/docs/features/accounts/create/#standalone
        } catch (Exception e) {
            e.printStackTrace();
            throw new Exception("Account creation error " + e.getMessage() );
        }
   
    }
    
    @GetMapping(path="/balance",produces = "application/json; charset=UTF-8")
    private String printBalance(com.algorand.algosdk.account.Account myAccount, @RequestParam Address address) throws Exception {
        Response < com.algorand.algosdk.v2.client.model.Account > respAcct = client.AccountInformation(address).execute();
       
        if (!respAcct.isSuccessful()) {
            throw new Exception(respAcct.message());
        }
        com.algorand.algosdk.v2.client.model.Account accountInfo = respAcct.body();
        System.out.println(String.format("Account Balance: %d microAlgos", accountInfo.amount));
        return accountInfo.amount.toString();
    }  
    
    @GetMapping(path="/balance2",produces = "application/json; charset=UTF-8")
    private String printBalance(@RequestParam String mnemonic) throws Exception {
    	Account account = new Account(mnemonic);
    	System.out.println(mnemonic+" "+ account.getAddress());
    	return printBalance(account,account.getAddress());
    }
    
    
    @GetMapping(path="/transaction",produces = "application/json; charset=UTF-8")
    public String firstTransaction(@RequestBody DataTrans datatrans) throws Exception {
    	
    	Account account = new Account(datatrans.getMnemonic());
    	String id = null;
    	printBalance(account ,account.getAddress());
     
        
        
        try {
            // Construct the transaction
            final String RECEIVER = datatrans.getReciver();
            String note = datatrans.getNote();
            Response<com.algorand.algosdk.v2.client.model.Account> respAcct = client.AccountInformation(account.getAddress()).execute();

            Response < TransactionParametersResponse > resp = client.TransactionParams().execute();
            if (!resp.isSuccessful()) {
                throw new Exception(resp.message());
            }
            TransactionParametersResponse params = resp.body();
            if (params == null) {
                throw new Exception("Params retrieval error");
            }
            JSONObject jsonObj = new JSONObject(params.toString());
            System.out.println("Algorand suggested parameters: " + jsonObj.toString(2));
            Transaction txn = Transaction.PaymentTransactionBuilder()
                .sender(account.getAddress())
                .note(note.getBytes())
                .amount(datatrans.getAmount()) // 1 algo = 1000000 microalgos
                .receiver(new Address(RECEIVER))
                .suggestedParams(params)
                .build();

            // Sign the transaction
            SignedTransaction signedTxn = account.signTransaction(txn);
            System.out.println("Signed transaction with txid: " + signedTxn.transactionID);

            // Submit the transaction to the network
            String[] headers = {"Content-Type"};
            String[] values = {"application/x-binary"};
            // Submit the transaction to the network
            byte[] encodedTxBytes = Encoder.encodeToMsgPack(signedTxn);
            Response < PostTransactionsResponse > rawtxresponse = client.RawTransaction().rawtxn(encodedTxBytes).execute(headers, values);
            if (!rawtxresponse.isSuccessful()) {
                throw new Exception(rawtxresponse.message());
            }
            id = rawtxresponse.body().txId;

            // Wait for transaction confirmation
            PendingTransactionResponse pTrx = Utils.waitForConfirmation(client, id, 4);

            System.out.println("Transaction " + id + " confirmed in round " + pTrx.confirmedRound);
            // Read the transaction
            JSONObject jsonObj2 = new JSONObject(pTrx.toString());
            System.out.println("Transaction information (with notes): " + jsonObj2.toString(2));
            System.out.println("Decoded note: " + new String(pTrx.txn.tx.note));
            System.out.println("Amount: " + new String(pTrx.txn.tx.amount.toString())); 
            System.out.println("Fee: " + new String(pTrx.txn.tx.fee.toString())); 
            if (pTrx.closingAmount != null){
             System.out.println("Closing Amount: " + new String(pTrx.closingAmount.toString()));                 
            }          
          

        } catch (Exception e) {
            System.err.println("Exception when calling algod#transactionInformation: " + e.getMessage());
        }
		
		
		return id;
    }
    
    //------------------------------------------------------------------------------------------------------
    
    
    
    
    private static final String SHA256_ALG = "SHA256";

    public static byte[] digest(byte[] data) throws NoSuchAlgorithmException {
        CryptoProvider.setupIfNeeded();
        java.security.MessageDigest digest = java.security.MessageDigest.getInstance(SHA256_ALG);
        digest.update(Arrays.copyOf(data, data.length));
        return digest.digest();
    }
    // utility function to print created asset
    public void printCreatedAsset(Account account, Long assetID) throws Exception {
        if (client == null)
            this.client = connectToNetwork();
        // String myAddress = account.getAddress().toString();
        Response<com.algorand.algosdk.v2.client.model.Account> respAcct = client
                .AccountInformation(account.getAddress()).execute();
        if (!respAcct.isSuccessful()) {
            throw new Exception(respAcct.message());
        }
        com.algorand.algosdk.v2.client.model.Account accountInfo = respAcct.body();
        JSONObject jsonObj = new JSONObject(accountInfo.toString());
        JSONArray jsonArray = (JSONArray) jsonObj.get("created-assets");
        if (jsonArray.length() > 0)
            try {
                for (Object o : jsonArray) {
                    JSONObject ca = (JSONObject) o;
                    Integer myassetIDInt = (Integer) ca.get("index");
                    if (assetID.longValue() == myassetIDInt.longValue()) {
                        System.out.println("Created Asset Info: " + ca.toString(2)); // pretty print
                        break;
                    }
                }
            } catch (Exception e) {
                throw (e);
            }
    }

    // utility function to print asset holding
    public void printAssetHolding(Account account, Long assetID) throws Exception {
        if (client == null)
            this.client = connectToNetwork();

        // String myAddress = account.getAddress().toString();
        Response<com.algorand.algosdk.v2.client.model.Account> respAcct = client
                .AccountInformation(account.getAddress()).execute();
        if (!respAcct.isSuccessful()) {
            throw new Exception(respAcct.message());
        }
        com.algorand.algosdk.v2.client.model.Account accountInfo = respAcct.body();
        JSONObject jsonObj = new JSONObject(accountInfo.toString());
        JSONArray jsonArray = (JSONArray) jsonObj.get("assets");
        if (jsonArray.length() > 0)
            try {
                for (Object o : jsonArray) {
                    JSONObject ca = (JSONObject) o;
                    Integer myassetIDInt = (Integer) ca.get("asset-id");
                    if (assetID.longValue() == myassetIDInt.longValue()) {
                        System.out.println("Asset Holding Info: " + ca.toString(2)); // pretty print
                        break;
                    }
                }
            } catch (Exception e) {
                throw (e);
            }
    }
    
    @GetMapping(path="/createNft",produces = "application/json; charset=UTF-8")
    public Long createNFTAsset(@RequestBody String mnemonic) throws Exception {
    	Account aliceAccount = new Account(mnemonic);
        System.out.println(String.format(""));
        System.out.println(String.format("==> CREATE ASSET"));    
        if (client == null)
            this.client = connectToNetwork();

        // get changing network parameters for each transaction
        Response<TransactionParametersResponse> resp = client.TransactionParams().execute();
        if (!resp.isSuccessful()) {
            throw new Exception(resp.message());
        }
        TransactionParametersResponse params = resp.body();
        if (params == null) {
            throw new Exception("Params retrieval error");
        }
        JSONObject jsonObj = new JSONObject(params.toString());
        System.out.println("Algorand suggested parameters: " + jsonObj.toString(2));

        // Create the Asset:

        boolean defaultFrozen = false;
        String unitName = "ALICEART";
        String assetName = "Alice's Artwork@arc3";
        String url = "";
        byte[] imageFile = Files.readAllBytes(Paths.get("C:\\Users\\danie\\Downloads\\AlgoDemo\\src\\main\\java\\NFT\\alice-nft.png"));
        byte[] imgHash = digest(imageFile); 
        String imgSRI = "sha256-" + Base64.getEncoder().encodeToString(imgHash);
        System.out.println("image_integrity : " + String.format(imgSRI));
        // Use imgSRI as the metadata  for "image_integrity": 
        // "sha256-/tih/7ew0eziEZIVD4qoTWb0YrElAuRG3b40SnEstyk=",

    
        byte[] metadataFILE = Files.readAllBytes(Paths.get("C:\\Users\\danie\\Downloads\\AlgoDemo\\src\\main\\java\\NFT\\metadata.json"));
        // use this to verify that the metadatahash displayed in the asset creation response is correct
        // cat metadata.json | openssl dgst -sha256 -binary | openssl base64 -A     
        byte[] assetMetadataHash = digest(metadataFILE); 
        // String assetMetadataSRI = Base64.getEncoder().encodeToString(assetMetadataHash);
        // System.out.println(String.format(assetMetadataSRI));

       
        String assetMetadataHashString = "16efaa3924a6fd9d3a4824799a4ac65d";

      

        Address manager = aliceAccount.getAddress();  // OPTIONAL: FOR DEMO ONLY, USED TO DESTROY ASSET WITHIN THIS SCRIPT
        Address reserve = null;
        Address freeze = null;
        Address clawback = null;

        // set quantity and decimal placement
        BigInteger assetTotal = BigInteger.valueOf(1);
        // decimals and assetTotal
        Integer decimals = 0;

        Transaction tx = Transaction.AssetCreateTransactionBuilder()
                .sender(aliceAccount.getAddress().toString())
                .assetTotal(assetTotal)
                .assetDecimals(decimals)
                .assetUnitName(unitName)
                .assetName(assetName)
                .url(url)
                .metadataHashUTF8(assetMetadataHashString)
                .manager(manager)
                .reserve(reserve)
                .freeze(freeze)
                .defaultFrozen(defaultFrozen)
                .clawback(clawback)
                .suggestedParams(params)
                .build();

        // Sign the Transaction with creator account
        SignedTransaction signedTxn = aliceAccount.signTransaction(tx);
        Long assetID = null;
        try {
            // Submit the transaction to the network
            String[] headers = { "Content-Type" };
            String[] values = { "application/x-binary" };
            // Submit the transaction to the network
            byte[] encodedTxBytes = Encoder.encodeToMsgPack(signedTxn);
            Response<PostTransactionsResponse> rawtxresponse = client.RawTransaction().rawtxn(encodedTxBytes)
                    .execute(headers, values);
            if (!rawtxresponse.isSuccessful()) {
                throw new Exception(rawtxresponse.message());
            }
            String id = rawtxresponse.body().txId;

            // Wait for transaction confirmation
            PendingTransactionResponse pTrx = Utils.waitForConfirmation(client,id,4);          
            System.out.println("Transaction " + id + " confirmed in round " + pTrx.confirmedRound);

            assetID = pTrx.assetIndex;
            System.out.println("AssetID = " + assetID);
            printCreatedAsset(aliceAccount, assetID);
            printAssetHolding(aliceAccount, assetID);
            return assetID;
        } catch (Exception e) {
            e.printStackTrace();
            return assetID;
        }

    }
    



}