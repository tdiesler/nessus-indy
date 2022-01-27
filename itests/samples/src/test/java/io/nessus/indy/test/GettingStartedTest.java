/*
 * #%L
 * Wildfly Camel :: Testsuite
 * %%
 * Copyright (C) 2013 - 2014 RedHat
 * %%
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * #L%
 */

package io.nessus.indy.test;

import static io.nessus.indy.utils.IndyConstants.ROLE_ENDORSER;
import static io.nessus.indy.utils.IndyConstants.ROLE_STEWARD;

import java.util.Arrays;

import org.hyperledger.indy.sdk.anoncreds.Anoncreds;
import org.hyperledger.indy.sdk.anoncreds.AnoncredsResults.IssuerCreateAndStoreCredentialDefResult;
import org.hyperledger.indy.sdk.anoncreds.AnoncredsResults.IssuerCreateSchemaResult;
import org.hyperledger.indy.sdk.did.Did;
import org.hyperledger.indy.sdk.did.DidResults.CreateAndStoreMyDidResult;
import org.hyperledger.indy.sdk.ledger.Ledger;
import org.hyperledger.indy.sdk.ledger.LedgerResults.ParseResponseResult;
import org.hyperledger.indy.sdk.pool.Pool;
import org.hyperledger.indy.sdk.wallet.Wallet;
import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.nessus.indy.utils.IndyConstants;
import io.nessus.indy.utils.PoolUtils;


public class GettingStartedTest {

	Logger log = LoggerFactory.getLogger(getClass());
	
	@Test
	public void testWorkflow() throws Exception {
		
		logInfo("LD_LIBRARY_PATH: {}", System.getenv("LD_LIBRARY_PATH"));
		
		logInfo("Ledger sample -> started");

		// Set protocol version 2
		Pool.setProtocolVersion(IndyConstants.PROTOCOL_VERSION).get();
		
		// 1. Create ledger config from genesis txn file
		logInfo("Create and open Ledger");
		String poolName = PoolUtils.createPoolLedgerConfig("pool1");
		Pool pool = Pool.openPoolLedger(poolName, "{}").get();

		// 2. Create the Wallets
		logInfo("Create wallet - Steward");
		String stewardWalletConfig = new JSONObject().put("id", "Steward").toString();
		String stewardWalletKey = new JSONObject().put("key", "steward_wallet_key").toString();
		Wallet.createWallet(stewardWalletConfig, stewardWalletKey).get();
		
		logInfo("Create wallet - Government");
		String governmentWalletConfig = new JSONObject().put("id", "Government").toString();
		String governmentWalletKey = new JSONObject().put("key", "government_wallet_key").toString();
		Wallet.createWallet(governmentWalletConfig, governmentWalletKey).get();
		
		logInfo("Create wallet - Faber");
		String faberWalletConfig = new JSONObject().put("id", "Faber").toString();
		String faberWalletKey = new JSONObject().put("key", "faber_wallet_key").toString();
		Wallet.createWallet(faberWalletConfig, faberWalletKey).get();
		
		logInfo("Create wallet - Acme");
		String acmeWalletConfig = new JSONObject().put("id", "Acme").toString();
		String acmeWalletKey = new JSONObject().put("key", "acme_wallet_key").toString();
		Wallet.createWallet(acmeWalletConfig, acmeWalletKey).get();
		
		logInfo("Create wallet - Thrift");
		String thriftWalletConfig = new JSONObject().put("id", "Thrift").toString();
		String thriftWalletKey = new JSONObject().put("key", "thrift_wallet_key").toString();
		Wallet.createWallet(thriftWalletConfig, thriftWalletKey).get();
		
		// 3. Getting Credential for Steward
		
		logInfo("Open wallet - Steward");
		Wallet stewardWallet = Wallet.openWallet(stewardWalletConfig, stewardWalletKey).get();
		String stewardSeed = new JSONObject().put("seed", "000000000000000000000000Steward1").toString();
		CreateAndStoreMyDidResult stewardDidResult = Did.createAndStoreMyDid(stewardWallet, stewardSeed).get();
		String stewardDid = stewardDidResult.getDid();
		String stewardVkey = stewardDidResult.getVerkey();
		logInfo("DID Steward: did={}, vkey={}", stewardDid, stewardVkey);
		String nymRequest = Ledger.buildNymRequest(stewardDid, stewardDid, stewardVkey, null, ROLE_STEWARD).get();
		Ledger.signAndSubmitRequest(pool, stewardWallet, stewardDid, nymRequest).get();

		// 4. Getting Credentials for Government, Faber, Acme, Thrift
		
		Wallet governmentWallet = Wallet.openWallet(governmentWalletConfig, governmentWalletKey).get();
		String governmentSeed = new JSONObject().put("seed", "000000000000000000000Government1").toString();
		CreateAndStoreMyDidResult governmentDidResult = Did.createAndStoreMyDid(governmentWallet, governmentSeed).get();
		String governmentDid = governmentDidResult.getDid();
		String governmentVkey = governmentDidResult.getVerkey();
		logInfo("DID Government: did={}, vkey={}", governmentDid, governmentVkey);
		nymRequest = Ledger.buildNymRequest(stewardDid, governmentDid, governmentVkey, null, ROLE_ENDORSER).get();
		Ledger.signAndSubmitRequest(pool, governmentWallet, governmentDid, nymRequest).get();

		Wallet faberWallet = Wallet.openWallet(faberWalletConfig, faberWalletKey).get();
		String faberSeed = new JSONObject().put("seed", "00000000000000000000000000Faber1").toString();
		CreateAndStoreMyDidResult faberDidResult = Did.createAndStoreMyDid(faberWallet, faberSeed).get();
		String faberDid = faberDidResult.getDid();
		String faberVkey = faberDidResult.getVerkey();
		logInfo("DID Faber: did={}, vkey={}", faberDid, faberVkey);
		nymRequest = Ledger.buildNymRequest(stewardDid, faberDid, faberVkey, null, ROLE_ENDORSER).get();
		Ledger.signAndSubmitRequest(pool, faberWallet, faberDid, nymRequest).get();

		Wallet acmeWallet = Wallet.openWallet(acmeWalletConfig, acmeWalletKey).get();
		String acmeSeed = new JSONObject().put("seed", "000000000000000000000000000Acme1").toString();
		CreateAndStoreMyDidResult acmeDidResult = Did.createAndStoreMyDid(acmeWallet, acmeSeed).get();
		String acmeDid = acmeDidResult.getDid();
		String acmeVkey = acmeDidResult.getVerkey();
		logInfo("DID Acme: did={}, vkey={}", acmeDid, acmeVkey);
		nymRequest = Ledger.buildNymRequest(stewardDid, acmeDid, acmeVkey, null, ROLE_ENDORSER).get();
		Ledger.signAndSubmitRequest(pool, acmeWallet, acmeDid, nymRequest).get();

		Wallet thriftWallet = Wallet.openWallet(thriftWalletConfig, thriftWalletKey).get();
		String thriftSeed = new JSONObject().put("seed", "0000000000000000000000000Thrift1").toString();
		CreateAndStoreMyDidResult thriftDidResult = Did.createAndStoreMyDid(thriftWallet, thriftSeed).get();
		String thriftDid = thriftDidResult.getDid();
		String thriftVkey = thriftDidResult.getVerkey();
		logInfo("DID Thrift: did={}, vkey={}", thriftDid, thriftVkey);
		nymRequest = Ledger.buildNymRequest(stewardDid, thriftDid, thriftVkey, null, ROLE_ENDORSER).get();
        Ledger.signAndSubmitRequest(pool, thriftWallet, thriftDid, nymRequest).get();

		// 5. Credential Schemas Setup for Job-Certificate and Transcript
		//
		// Schemas in indy are very simple JSON documents that specify their name and version, and that list attributes that will appear in a credential.
		// Today, they do not describe data type, recurrence rules, nesting, and other elaborate constructs.

        // Build and submit the Job-Certificate schema
        
		IssuerCreateSchemaResult schemaResult = Anoncreds.issuerCreateSchema(stewardDid, "Job-Certificate", "0.2", new JSONArray(Arrays.asList("first_name","last_name","salary","employee_status","experience")).toString()).get();
		logInfo(schemaResult.toString());
		Ledger.buildSchemaRequest(stewardDid, schemaResult.getSchemaJson()).get();
		String jobCertificateSchemaId = schemaResult.getSchemaId();
		
		String schemaRequest = Ledger.buildSchemaRequest(stewardDid, schemaResult.getSchemaJson()).get();
		Ledger.signAndSubmitRequest(pool, stewardWallet, stewardDid, schemaRequest).get();
		
		String getSchemaRequest = Ledger.buildGetSchemaRequest(stewardDid, jobCertificateSchemaId).get();
		String jobCertificateGetSchemaResponse = Ledger.submitRequest(pool, getSchemaRequest).get();
		logInfo(jobCertificateGetSchemaResponse);
		
        // Build and submit the Transcript schema
		
		schemaResult = Anoncreds.issuerCreateSchema(stewardDid, "Transcript", "1.2", new JSONArray(Arrays.asList("first_name","last_name","degree","status","year","average","ssn")).toString()).get();		
		logInfo(schemaResult.toString());
		Ledger.buildSchemaRequest(stewardDid, schemaResult.getSchemaJson()).get();
		String transcriptSchemaId = schemaResult.getSchemaId();
		
		schemaRequest = Ledger.buildSchemaRequest(stewardDid, schemaResult.getSchemaJson()).get();
		Ledger.signAndSubmitRequest(pool, stewardWallet, stewardDid, schemaRequest).get();
		
		getSchemaRequest = Ledger.buildGetSchemaRequest(stewardDid, transcriptSchemaId).get();
		String transcriptGetSchemaResponse = Ledger.submitRequest(pool, getSchemaRequest).get();
		logInfo(transcriptGetSchemaResponse);
				
		// 6. Credential Definition Setup for Faber and Acme
		//
		// References the schema that we just added, and announces who is going to be issuing credentials with that schema,
		// what type of signature method they plan to use (“CL” = “Camenisch Lysyanskya”, the default method used for zero-knowledge proofs by indy),
		// how they plan to handle revocation, and so forth.
		
		// Faber Credential Definition Setup
		
		String configJson = new JSONObject().put("support_revocation", false).toString();
		ParseResponseResult parseSchemaResult = Ledger.parseGetSchemaResponse(transcriptGetSchemaResponse).get();
		IssuerCreateAndStoreCredentialDefResult createCredDefResult = Anoncreds.issuerCreateAndStoreCredentialDef(faberWallet, faberDid, parseSchemaResult.getObjectJson(), "TAG1", null, configJson).get();
		String transcriptCredDefId = createCredDefResult.getCredDefId();

		String credDefRequest = Ledger.buildCredDefRequest(faberDid, createCredDefResult.getCredDefJson()).get();
		Ledger.signAndSubmitRequest(pool, faberWallet, faberDid, credDefRequest).get();
		
		String getCredDefRequest = Ledger.buildGetCredDefRequest(faberDid, transcriptCredDefId).get();
		String transcriptGetCredDefResponse = Ledger.submitRequest(pool, getCredDefRequest).get();
		logInfo(transcriptGetCredDefResponse);
		
		// 7. Acme Credential Definition Setup

		parseSchemaResult = Ledger.parseGetSchemaResponse(jobCertificateGetSchemaResponse).get();
		createCredDefResult = Anoncreds.issuerCreateAndStoreCredentialDef(acmeWallet, acmeDid, parseSchemaResult.getObjectJson(), "TAG1", null, configJson).get();
		String jobCertificateCredDefId = createCredDefResult.getCredDefId();

		credDefRequest = Ledger.buildCredDefRequest(acmeDid, createCredDefResult.getCredDefJson()).get();
		Ledger.signAndSubmitRequest(pool, acmeWallet, acmeDid, credDefRequest).get();
		
		getCredDefRequest = Ledger.buildGetCredDefRequest(acmeDid, jobCertificateCredDefId).get();
		String jobCertificateGetCredDefResponse = Ledger.submitRequest(pool, getCredDefRequest).get();
		logInfo(jobCertificateGetCredDefResponse);

		// ...
		
		// 9. Close and delete the Wallets
		thriftWallet.closeWallet().get();
		acmeWallet.closeWallet().get();
		faberWallet.closeWallet().get();
		governmentWallet.closeWallet().get();
		stewardWallet.closeWallet().get();

		Wallet.deleteWallet(thriftWalletConfig, thriftWalletKey).get();
		Wallet.deleteWallet(acmeWalletConfig, acmeWalletKey).get();
		Wallet.deleteWallet(faberWalletConfig, faberWalletKey).get();
		Wallet.deleteWallet(governmentWalletConfig, governmentWalletKey).get();
		Wallet.deleteWallet(stewardWalletConfig, stewardWalletKey).get();
		
		// 10. Close Pool
		pool.closePoolLedger().get();

		// 11. Delete Pool ledger config
		Pool.deletePoolLedgerConfig(poolName).get();

		logInfo("Ledger sample -> completed");
	}

	private void logInfo(String msg, Object... args) {
		log.info(msg, args);
	}
}
