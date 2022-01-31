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
import org.hyperledger.indy.sdk.anoncreds.AnoncredsResults.IssuerCreateSchemaResult;
import org.hyperledger.indy.sdk.did.Did;
import org.hyperledger.indy.sdk.did.DidResults.CreateAndStoreMyDidResult;
import org.hyperledger.indy.sdk.ledger.Ledger;
import org.hyperledger.indy.sdk.pool.Pool;
import org.hyperledger.indy.sdk.wallet.Wallet;
import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.nessus.indy.utils.IndyConstants;
import io.nessus.indy.utils.PoolUtils;

/**
 * Start a local indy pool
 * 
 * docker rm -f indy-pool
 * docker run --detach --name=indy-pool -p 9701-9708:9701-9708 nessusio/indy-pool
 * 
 * Remove dirty client state
 * 
 * rm -rf ~/.indy_client
 */
public class GettingStartedTest {

	Logger log = LoggerFactory.getLogger(getClass());
	
	class Context {
		
		// Pool Ledger
		String poolName;
		Pool pool;
		
		// Steward
		String stewardWalletConfig;
		String stewardWalletKey;
		Wallet stewardWallet;
		String stewardDid;
		String stewardVkey;
		String stewardDidForGovernment;
		String stewardVkeyForGovernment;
		String stewardDidForFaber;
		String stewardVkeyForFaber;
		String stewardDidForAcme;
		String stewardVkeyForAcme;
		String stewardDidForThrift;
		String stewardVkeyForThrift;
		
		// Government
		String governmentWalletConfig;
		String governmentWalletKey;
		Wallet governmentWallet;
		String governmentDid;
		String governmentVkey;
		String governmentDidForSteward;
		String governmentVkeyForSteward;
		
		// Faber
		String faberWalletConfig;
		String faberWalletKey;
		Wallet faberWallet;
		String faberDid;
		String faberVkey;
		String faberDidForSteward;
		String faberVkeyForSteward;
		
		// Acme
		String acmeWalletConfig;
		String acmeWalletKey;
		Wallet acmeWallet;
		String acmeDid;
		String acmeVkey;
		String acmeDidForSteward;
		String acmeVkeyForSteward;
		
		// Thrift
		String thriftWalletConfig;
		String thriftWalletKey;
		Wallet thriftWallet;
		String thriftDid;
		String thriftVkey;
		String thriftDidForSteward;
		String thriftVkeyForSteward;
		
	}
	
	@Test
	public void testWorkflow() throws Exception {
		
		Context ctx = new Context();
		
		// Setup Indy Pool Nodes
		
		createAndOpenPoolLedger(ctx);
		
		// Creating Steward Wallet and DID
		
		createSteward(ctx);
		
		// Onboarding Government, Faber, Acme, Thrift as Trust Anchors
		
		onboardGovernment(ctx);
		onboardFaberCollege(ctx);
		onboardAcmeCorp(ctx);
		onboardThriftBank(ctx);
		
		// Creating Credential Schemas
		
		createTranscriptSchema(ctx);
		createJobCertificateSchema(ctx);
		
		// Creating Credential Definitions
		
		
		// Close and Delete Indy Pool Nodes
		
		closeAndDeletePoolLedger(ctx);
	}

	void createAndOpenPoolLedger(Context ctx) throws Exception {
		
		logInfo("Create and Open Pool Ledger");

		logInfo("LD_LIBRARY_PATH: {}", System.getenv("LD_LIBRARY_PATH"));
		
		// Set protocol version 2
		Pool.setProtocolVersion(IndyConstants.PROTOCOL_VERSION).get();
		
		// Create ledger config from genesis txn file
		
		logInfo("Create and open Ledger");
		ctx.poolName = PoolUtils.createPoolLedgerConfig("pool1");
		ctx.pool = Pool.openPoolLedger(ctx.poolName, "{}").get();
	}
	
	void createSteward(Context ctx) throws Exception {
		
		// Crearte Wallet for Steward
		logInfo("Create wallet - Steward");
		ctx.stewardWalletConfig = new JSONObject().put("id", "Steward").toString();
		ctx.stewardWalletKey = new JSONObject().put("key", "steward_wallet_key").toString();
		Wallet.createWallet(ctx.stewardWalletConfig, ctx.stewardWalletKey).get();
		
		// Getting Credential for Steward
		
		logInfo("Open wallet - Steward");
		ctx.stewardWallet = Wallet.openWallet(ctx.stewardWalletConfig, ctx.stewardWalletKey).get();
		String stewardSeed = new JSONObject().put("seed", "000000000000000000000000Steward1").toString();
		CreateAndStoreMyDidResult didResult = Did.createAndStoreMyDid(ctx.stewardWallet, stewardSeed).get();
		ctx.stewardDid = didResult.getDid();
		ctx.stewardVkey = didResult.getVerkey();
	}
	
	void onboardGovernment(Context ctx) throws Exception {
		
		CreateAndStoreMyDidResult didResult = Did.createAndStoreMyDid(ctx.stewardWallet, "{}").get();
		ctx.stewardDidForGovernment = didResult.getDid();
		ctx.stewardVkeyForGovernment = didResult.getVerkey();

		logInfo("DID StewardForGovernment: did={}, vkey={}", ctx.stewardDidForGovernment, ctx.stewardVkeyForGovernment);
		String nymRequest = Ledger.buildNymRequest(ctx.stewardDidForGovernment, ctx.stewardDidForGovernment, ctx.stewardVkeyForGovernment, null, ROLE_STEWARD).get();
		Ledger.signAndSubmitRequest(ctx.pool, ctx.stewardWallet, ctx.stewardDidForGovernment, nymRequest).get();

		logInfo("Create wallet - Government");
		ctx.governmentWalletConfig = new JSONObject().put("id", "Government").toString();
		ctx.governmentWalletKey = new JSONObject().put("key", "government_wallet_key").toString();
		Wallet.createWallet(ctx.governmentWalletConfig, ctx.governmentWalletKey).get();
		
		ctx.governmentWallet = Wallet.openWallet(ctx.governmentWalletConfig, ctx.governmentWalletKey).get();
		didResult = Did.createAndStoreMyDid(ctx.governmentWallet, "{}").get();
		ctx.governmentDidForSteward = didResult.getDid();
		ctx.governmentVkeyForSteward = didResult.getVerkey();
		logInfo("DID GovernmentForSteward: did={}, vkey={}", ctx.governmentDidForSteward, ctx.governmentVkeyForSteward);
		nymRequest = Ledger.buildNymRequest(ctx.stewardDidForGovernment, ctx.governmentDidForSteward, ctx.governmentVkeyForSteward, null, ROLE_ENDORSER).get();
		Ledger.signAndSubmitRequest(ctx.pool, ctx.governmentWallet, ctx.governmentDidForSteward, nymRequest).get();
		
		String governmentSeed = new JSONObject().put("seed", "000000000000000000000Government1").toString();
		didResult = Did.createAndStoreMyDid(ctx.governmentWallet, governmentSeed).get();
		ctx.governmentDid = didResult.getDid();
		ctx.governmentVkey = didResult.getVerkey();
		logInfo("DID Government: did={}, vkey={}", ctx.governmentDid, ctx.governmentVkey);
	}
	
	void onboardFaberCollege(Context ctx) throws Exception {
		
		CreateAndStoreMyDidResult didResult = Did.createAndStoreMyDid(ctx.stewardWallet, "{}").get();
		ctx.stewardDidForFaber = didResult.getDid();
		ctx.stewardVkeyForFaber = didResult.getVerkey();

		logInfo("DID StewardForFaber: did={}, vkey={}", ctx.stewardDidForFaber, ctx.stewardVkeyForFaber);
		String nymRequest = Ledger.buildNymRequest(ctx.stewardDidForFaber, ctx.stewardDidForFaber, ctx.stewardVkeyForFaber, null, ROLE_STEWARD).get();
		Ledger.signAndSubmitRequest(ctx.pool, ctx.stewardWallet, ctx.stewardDidForFaber, nymRequest).get();

		logInfo("Create wallet - Faber");
		ctx.faberWalletConfig = new JSONObject().put("id", "Faber").toString();
		ctx.faberWalletKey = new JSONObject().put("key", "faber_wallet_key").toString();
		Wallet.createWallet(ctx.faberWalletConfig, ctx.faberWalletKey).get();
		
		ctx.faberWallet = Wallet.openWallet(ctx.faberWalletConfig, ctx.faberWalletKey).get();
		didResult = Did.createAndStoreMyDid(ctx.faberWallet, "{}").get();
		ctx.faberDidForSteward = didResult.getDid();
		ctx.faberVkeyForSteward = didResult.getVerkey();
		logInfo("DID FaberForSteward: did={}, vkey={}", ctx.faberDidForSteward, ctx.faberVkeyForSteward);
		nymRequest = Ledger.buildNymRequest(ctx.stewardDidForFaber, ctx.faberDidForSteward, ctx.faberVkeyForSteward, null, ROLE_ENDORSER).get();
		Ledger.signAndSubmitRequest(ctx.pool, ctx.faberWallet, ctx.faberDidForSteward, nymRequest).get();

		String faberSeed = new JSONObject().put("seed", "00000000000000000000000000Faber1").toString();
		CreateAndStoreMyDidResult faberDidResult = Did.createAndStoreMyDid(ctx.faberWallet, faberSeed).get();
		ctx.faberDid = faberDidResult.getDid();
		ctx.faberVkey = faberDidResult.getVerkey();
	}
	
	void onboardAcmeCorp(Context ctx) throws Exception {
		
		CreateAndStoreMyDidResult didResult = Did.createAndStoreMyDid(ctx.stewardWallet, "{}").get();
		ctx.stewardDidForAcme = didResult.getDid();
		ctx.stewardVkeyForAcme = didResult.getVerkey();

		logInfo("DID StewardForAcme: did={}, vkey={}", ctx.stewardDidForAcme, ctx.stewardVkeyForAcme);
		String nymRequest = Ledger.buildNymRequest(ctx.stewardDidForAcme, ctx.stewardDidForAcme, ctx.stewardVkeyForAcme, null, ROLE_STEWARD).get();
		Ledger.signAndSubmitRequest(ctx.pool, ctx.stewardWallet, ctx.stewardDidForAcme, nymRequest).get();

		logInfo("Create wallet - Acme");
		ctx.acmeWalletConfig = new JSONObject().put("id", "Acme").toString();
		ctx.acmeWalletKey = new JSONObject().put("key", "acme_wallet_key").toString();
		Wallet.createWallet(ctx.acmeWalletConfig, ctx.acmeWalletKey).get();
		
		ctx.acmeWallet = Wallet.openWallet(ctx.acmeWalletConfig, ctx.acmeWalletKey).get();
		didResult = Did.createAndStoreMyDid(ctx.acmeWallet, "{}").get();
		ctx.acmeDidForSteward = didResult.getDid();
		ctx.acmeVkeyForSteward = didResult.getVerkey();
		logInfo("DID AcmeForSteward: did={}, vkey={}", ctx.acmeDidForSteward, ctx.acmeVkeyForSteward);
		nymRequest = Ledger.buildNymRequest(ctx.stewardDidForAcme, ctx.acmeDidForSteward, ctx.acmeVkeyForSteward, null, ROLE_ENDORSER).get();
		Ledger.signAndSubmitRequest(ctx.pool, ctx.acmeWallet, ctx.acmeDidForSteward, nymRequest).get();

		String acmeSeed = new JSONObject().put("seed", "000000000000000000000000000Acme1").toString();
		didResult = Did.createAndStoreMyDid(ctx.acmeWallet, acmeSeed).get();
		ctx.acmeDid = didResult.getDid();
		ctx.acmeVkey = didResult.getVerkey();
	}
	
	void onboardThriftBank(Context ctx) throws Exception {
		
		CreateAndStoreMyDidResult didResult = Did.createAndStoreMyDid(ctx.stewardWallet, "{}").get();
		ctx.stewardDidForThrift = didResult.getDid();
		ctx.stewardVkeyForThrift = didResult.getVerkey();

		logInfo("DID StewardForThrift: did={}, vkey={}", ctx.stewardDidForThrift, ctx.stewardVkeyForThrift);
		String nymRequest = Ledger.buildNymRequest(ctx.stewardDidForThrift, ctx.stewardDidForThrift, ctx.stewardVkeyForThrift, null, ROLE_STEWARD).get();
		Ledger.signAndSubmitRequest(ctx.pool, ctx.stewardWallet, ctx.stewardDidForThrift, nymRequest).get();

		logInfo("Create wallet - Thrift");
		ctx.thriftWalletConfig = new JSONObject().put("id", "Thrift").toString();
		ctx.thriftWalletKey = new JSONObject().put("key", "thrift_wallet_key").toString();
		Wallet.createWallet(ctx.thriftWalletConfig, ctx.thriftWalletKey).get();
		
		ctx.thriftWallet = Wallet.openWallet(ctx.thriftWalletConfig, ctx.thriftWalletKey).get();
		didResult = Did.createAndStoreMyDid(ctx.thriftWallet, "{}").get();
		ctx.thriftDidForSteward = didResult.getDid();
		ctx.thriftVkeyForSteward = didResult.getVerkey();
		logInfo("DID FaberForSteward: did={}, vkey={}", ctx.thriftDidForSteward, ctx.thriftVkeyForSteward);
		nymRequest = Ledger.buildNymRequest(ctx.stewardDidForFaber, ctx.thriftDidForSteward, ctx.thriftVkeyForSteward, null, ROLE_ENDORSER).get();
		Ledger.signAndSubmitRequest(ctx.pool, ctx.thriftWallet, ctx.thriftDidForSteward, nymRequest).get();

		String thriftSeed = new JSONObject().put("seed", "0000000000000000000000000Thrift1").toString();
		didResult = Did.createAndStoreMyDid(ctx.thriftWallet, thriftSeed).get();
		ctx.thriftDidForSteward = didResult.getDid();
		ctx.thriftVkeyForSteward = didResult.getVerkey();
	}
	
	void createTranscriptSchema(Context ctx) throws Exception {
		
		// Schemas in indy are very simple JSON documents that specify their name and version, and that list attributes that will appear in a credential.
		// Today, they do not describe data type, recurrence rules, nesting, and other elaborate constructs.
		
		IssuerCreateSchemaResult schemaResult = Anoncreds.issuerCreateSchema(ctx.stewardDid, "Transcript", "1.2", new JSONArray(Arrays.asList("first_name","last_name","degree","status","year","average","ssn")).toString()).get();		
		logInfo(schemaResult.toString());
		Ledger.buildSchemaRequest(ctx.governmentDid, schemaResult.getSchemaJson()).get();
		String schemaId = schemaResult.getSchemaId();
		
		String schemaRequest = Ledger.buildSchemaRequest(ctx.governmentDid, schemaResult.getSchemaJson()).get();
		Ledger.signAndSubmitRequest(ctx.pool, ctx.governmentWallet, ctx.governmentDid, schemaRequest).get();
		
		String getSchemaRequest = Ledger.buildGetSchemaRequest(ctx.governmentDid, schemaId).get();
		String schemaResponse = Ledger.submitRequest(ctx.pool, getSchemaRequest).get();
		logInfo(schemaResponse);
	}
	
	void createJobCertificateSchema(Context ctx) throws Exception {
		
		IssuerCreateSchemaResult schemaResult = Anoncreds.issuerCreateSchema(ctx.governmentDid, "Job-Certificate", "0.2", new JSONArray(Arrays.asList("first_name","last_name","salary","employee_status","experience")).toString()).get();
		logInfo(schemaResult.toString());
		Ledger.buildSchemaRequest(ctx.governmentDid, schemaResult.getSchemaJson()).get();
		String schemaId = schemaResult.getSchemaId();
		
		String schemaRequest = Ledger.buildSchemaRequest(ctx.governmentDid, schemaResult.getSchemaJson()).get();
		Ledger.signAndSubmitRequest(ctx.pool, ctx.governmentWallet, ctx.governmentDid, schemaRequest).get();
		
		String getSchemaRequest = Ledger.buildGetSchemaRequest(ctx.governmentDid, schemaId).get();
		String schemaResponse = Ledger.submitRequest(ctx.pool, getSchemaRequest).get();
		logInfo(schemaResponse);
	}
	
	void closeAndDeletePoolLedger(Context ctx) throws Exception {
		
		logInfo("Close Wallets");
		
		closeAndDeleteWallet(ctx.thriftWallet, ctx.thriftWalletConfig, ctx.thriftWalletKey);
		closeAndDeleteWallet(ctx.acmeWallet, ctx.acmeWalletConfig, ctx.acmeWalletKey);
		closeAndDeleteWallet(ctx.faberWallet, ctx.faberWalletConfig, ctx.faberWalletKey);
		closeAndDeleteWallet(ctx.governmentWallet, ctx.governmentWalletConfig, ctx.governmentWalletKey);
		closeAndDeleteWallet(ctx.stewardWallet, ctx.stewardWalletConfig, ctx.stewardWalletKey);
		
		logInfo("Close and Delete Pool Ledger");

		ctx.pool.closePoolLedger().get();
		Pool.deletePoolLedgerConfig(ctx.poolName).get();
	}

	void closeAndDeleteWallet(Wallet wallet, String config, String key) throws Exception {
		if (wallet != null) {
			wallet.closeWallet().get();
			Wallet.deleteWallet(config, key).get();
		}
	}
	
	private void logInfo(String msg, Object... args) {
		log.info(msg, args);
	}
}
