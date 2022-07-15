package it.atlantica.AlgoDemo.Service;

import java.util.Scanner;

import org.springframework.stereotype.Service;

import com.algorand.algosdk.account.Account;
@Service
public class GettingStarted{
    // Create Account
    static Scanner scan = new Scanner(System.in);
    public Account createAccount()  throws Exception {
        try {
            Account myAccount1 = new Account();
            System.out.println("My Address: " + myAccount1.getAddress());
            System.out.println("My Passphrase: " + myAccount1.toMnemonic());
            System.out.println("Navigate to this link and dispense funds:  https://dispenser.testnet.aws.algodev.network?account=" + myAccount1.getAddress().toString());            

            System.out.println("PRESS ENTER KEY TO CONTINUE...");
           
            return myAccount1;
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
}
