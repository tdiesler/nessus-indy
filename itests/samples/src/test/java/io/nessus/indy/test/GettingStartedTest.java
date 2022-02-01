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
import static io.nessus.indy.utils.IndyConstants.ROLE_TRUSTEE;

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

/**
 * Start a local indy pool
 * 
 * docker rm -f indy-pool
 * docker run --detach --name=indy-pool -p 9701-9708:9701-9708 nessusio/indy-pool
 * 
 * Remove dirty client state
 * 
 * rm -rf ~/.indy_client
 * 
 * Trustees operate nodes. Trustees govern the network. These are the highest privileged DIDs. 
 * Endorsers are able to write Schemas and Cred_Defs to the ledger, or sign such transactions so they can be written by non-privileged DIDs.
 * 
 * We want to ensure a DID has the least amount of privilege it needs to operate, which in many cases is no privilege, 
 * provided the resources it needs are already written to the ledger, either by a privileged DID or by having the txn signed by a privileged DID (e.g. by an Endorser).
 */
public class GettingStartedTest {

	Logger log = LoggerFactory.getLogger(getClass());
	
	class Context {
		
		// Pool Ledger
		String poolName;
		Pool pool;
		
		// Trustee
		String trusteeWalletConfig;
		String trusteeWalletKey;
		Wallet trusteeWallet;
		String trusteeDid;
		String trusteeVkey;
		
		// Government
		String governmentWalletConfig;
		String governmentWalletKey;
		Wallet governmentWallet;
		String governmentDid;
		String governmentVkey;
		
		// Faber
		String faberWalletConfig;
		String faberWalletKey;
		Wallet faberWallet;
		String faberDid;
		String faberVkey;
		String faberTranscriptSchemaId;
		
		// Acme
		String acmeWalletConfig;
		String acmeWalletKey;
		Wallet acmeWallet;
		String acmeDid;
		String acmeVkey;
		String acmeJobCertificateSchemaId;
		
		// Thrift
		String thriftWalletConfig;
		String thriftWalletKey;
		Wallet thriftWallet;
		String thriftDid;
		String thriftVkey;
	}
	
	@Test
	public void testWorkflow() throws Exception {
		
		Context ctx = new Context();
		
		// Setup Indy Pool Nodes
		
		createAndOpenPoolLedger(ctx);
		
		// Creating Trustee Wallet and DID
		
		createTrustee(ctx);
		
		// Onboarding Government, Faber, Acme, Thrift as Trust Anchors
		
		onboardGovernment(ctx);
		onboardFaberCollege(ctx);
		onboardAcmeCorp(ctx);
		onboardThriftBank(ctx);
		
		// Creating Credential Schemas
		
		createTranscriptSchema(ctx);
		createJobCertificateSchema(ctx);
		
		// Creating Credential Definitions
		
		createTranscriptCredentialDefinition(ctx);
		createJobCertificateCredentialDefinition(ctx);
		
		// Close and Delete Indy Pool Nodes
		
		closeAndDeletePoolLedger(ctx);
	}

	void createAndOpenPoolLedger(Context ctx) throws Exception {
		
		log.info("Create and Open Pool Ledger");
		
		// Set protocol version 2
		Pool.setProtocolVersion(IndyConstants.PROTOCOL_VERSION).get();
		
		// Create ledger config from genesis txn file
		
		log.info("Create and open Ledger");
		ctx.poolName = PoolUtils.createPoolLedgerConfig("pool1");
		ctx.pool = Pool.openPoolLedger(ctx.poolName, "{}").get();
	}
	
	void createTrustee(Context ctx) throws Exception {
		
		// Create Wallet for Trustee
		
		log.info("Create wallet - Trustee");
		ctx.trusteeWalletConfig = new JSONObject().put("id", "Trustee").toString();
		ctx.trusteeWalletKey = new JSONObject().put("key", "trustee_wallet_key").toString();
		Wallet.createWallet(ctx.trusteeWalletConfig, ctx.trusteeWalletKey).get();
		ctx.trusteeWallet = Wallet.openWallet(ctx.trusteeWalletConfig, ctx.trusteeWalletKey).get();
		
		// Getting Credential for Trustee
		
		String trusteeSeed = new JSONObject().put("seed", "000000000000000000000000Trustee1").toString();
		CreateAndStoreMyDidResult didResult = Did.createAndStoreMyDid(ctx.trusteeWallet, trusteeSeed).get();
		ctx.trusteeDid = didResult.getDid();
		ctx.trusteeVkey = didResult.getVerkey();
		
		log.info("DID Trustee: did={}, vkey={}", ctx.trusteeDid, ctx.trusteeVkey);
		String nymRequest = Ledger.buildNymRequest(ctx.trusteeDid, ctx.trusteeDid, ctx.trusteeVkey, null, ROLE_TRUSTEE).get();
		signAndSubmitRequest(ctx, ctx.trusteeWallet, ctx.trusteeDid, nymRequest);
	}
	
	void onboardGovernment(Context ctx) throws Exception {
		
		// Create Wallet for Government
		
		log.info("Create wallet - Government");
		ctx.governmentWalletConfig = new JSONObject().put("id", "Government").toString();
		ctx.governmentWalletKey = new JSONObject().put("key", "government_wallet_key").toString();
		Wallet.createWallet(ctx.governmentWalletConfig, ctx.governmentWalletKey).get();
		ctx.governmentWallet = Wallet.openWallet(ctx.governmentWalletConfig, ctx.governmentWalletKey).get();
		
		// Getting Credential for Government
		
		String governmentSeed = new JSONObject().put("seed", "000000000000000000000Government1").toString();
		CreateAndStoreMyDidResult didResult = Did.createAndStoreMyDid(ctx.governmentWallet, governmentSeed).get();
		ctx.governmentDid = didResult.getDid();
		ctx.governmentVkey = didResult.getVerkey();
		
		log.info("DID Government: did={}, vkey={}", ctx.governmentDid, ctx.governmentVkey);
		String nymRequest = Ledger.buildNymRequest(ctx.trusteeDid, ctx.governmentDid, ctx.governmentVkey, null, ROLE_TRUSTEE).get();
		signAndSubmitRequest(ctx, ctx.trusteeWallet, ctx.trusteeDid, nymRequest);
	}
	
	void onboardFaberCollege(Context ctx) throws Exception {
		
		// Create Wallet for Faber
		
		log.info("Create wallet - Faber");
		ctx.faberWalletConfig = new JSONObject().put("id", "Faber").toString();
		ctx.faberWalletKey = new JSONObject().put("key", "faber_wallet_key").toString();
		Wallet.createWallet(ctx.faberWalletConfig, ctx.faberWalletKey).get();
		ctx.faberWallet = Wallet.openWallet(ctx.faberWalletConfig, ctx.faberWalletKey).get();
		
		// Getting Credential for Faber
		String faberSeed = new JSONObject().put("seed", "00000000000000000000000000Faber1").toString();
		CreateAndStoreMyDidResult didResult = Did.createAndStoreMyDid(ctx.faberWallet, faberSeed).get();
		ctx.faberDid = didResult.getDid();
		ctx.faberVkey = didResult.getVerkey();
		
		log.info("DID Faber: did={}, vkey={}", ctx.faberDid, ctx.faberVkey);
		String nymRequest = Ledger.buildNymRequest(ctx.governmentDid, ctx.faberDid, ctx.faberVkey, null, ROLE_ENDORSER).get();
		signAndSubmitRequest(ctx, ctx.governmentWallet, ctx.governmentDid, nymRequest);
	}
	
	void onboardAcmeCorp(Context ctx) throws Exception {
		
		// Create Wallet for Acme
		
		log.info("Create wallet - Acme");
		ctx.acmeWalletConfig = new JSONObject().put("id", "Acme").toString();
		ctx.acmeWalletKey = new JSONObject().put("key", "faber_wallet_key").toString();
		Wallet.createWallet(ctx.acmeWalletConfig, ctx.acmeWalletKey).get();
		ctx.acmeWallet = Wallet.openWallet(ctx.acmeWalletConfig, ctx.acmeWalletKey).get();
		
		// Getting Credential for Acme
		
		String acmeSeed = new JSONObject().put("seed", "000000000000000000000000000Acme1").toString();
		CreateAndStoreMyDidResult didResult = Did.createAndStoreMyDid(ctx.acmeWallet, acmeSeed).get();
		ctx.acmeDid = didResult.getDid();
		ctx.acmeVkey = didResult.getVerkey();
		
		log.info("DID Acme: did={}, vkey={}", ctx.acmeDid, ctx.acmeVkey);
		String nymRequest = Ledger.buildNymRequest(ctx.governmentDid, ctx.acmeDid, ctx.acmeVkey, null, ROLE_ENDORSER).get();
		signAndSubmitRequest(ctx, ctx.governmentWallet, ctx.governmentDid, nymRequest);
	}
	
	void onboardThriftBank(Context ctx) throws Exception {
		
		// Create Wallet for Thrift
		
		log.info("Create wallet - Thrift");
		ctx.thriftWalletConfig = new JSONObject().put("id", "Thrift").toString();
		ctx.thriftWalletKey = new JSONObject().put("key", "faber_wallet_key").toString();
		Wallet.createWallet(ctx.thriftWalletConfig, ctx.thriftWalletKey).get();
		ctx.thriftWallet = Wallet.openWallet(ctx.thriftWalletConfig, ctx.thriftWalletKey).get();
		
		// Getting Credential for Thrift
		
		String thriftSeed = new JSONObject().put("seed", "0000000000000000000000000Thrift1").toString();
		CreateAndStoreMyDidResult didResult = Did.createAndStoreMyDid(ctx.thriftWallet, thriftSeed).get();
		ctx.thriftDid = didResult.getDid();
		ctx.thriftVkey = didResult.getVerkey();
		
		log.info("DID Thrift: did={}, vkey={}", ctx.thriftDid, ctx.thriftVkey);
		String nymRequest = Ledger.buildNymRequest(ctx.governmentDid, ctx.thriftDid, ctx.thriftVkey, null, ROLE_ENDORSER).get();
		signAndSubmitRequest(ctx, ctx.governmentWallet, ctx.governmentDid, nymRequest);
	}
	
	void createTranscriptSchema(Context ctx) throws Exception {
		
		// Schemas in indy are very simple JSON documents that specify their name and version, and that list attributes that will appear in a credential.
		// Today, they do not describe data type, recurrence rules, nesting, and other elaborate constructs.
		
		Wallet issuerWallet = ctx.faberWallet;
		String issuerDid = ctx.faberDid;
		
		IssuerCreateSchemaResult schemaResult = Anoncreds.issuerCreateSchema(issuerDid, "Transcript", "1.2", new JSONArray(Arrays.asList("first_name","last_name","degree","status","year","average","ssn")).toString()).get();		
		log.info(schemaResult.toString());
		Ledger.buildSchemaRequest(issuerDid, schemaResult.getSchemaJson()).get();
		ctx.faberTranscriptSchemaId = schemaResult.getSchemaId();
		
		String schemaRequest = Ledger.buildSchemaRequest(issuerDid, schemaResult.getSchemaJson()).get();
		signAndSubmitRequest(ctx, issuerWallet, issuerDid, schemaRequest);
	}
	
	void createJobCertificateSchema(Context ctx) throws Exception {
		
		Wallet issuerWallet = ctx.acmeWallet;
		String issuerDid = ctx.acmeDid;
		
		IssuerCreateSchemaResult schemaResult = Anoncreds.issuerCreateSchema(issuerDid, "Job-Certificate", "0.2", new JSONArray(Arrays.asList("first_name","last_name","salary","employee_status","experience")).toString()).get();
		log.info(schemaResult.toString());
		Ledger.buildSchemaRequest(issuerDid, schemaResult.getSchemaJson()).get();
		ctx.acmeJobCertificateSchemaId = schemaResult.getSchemaId();
		
		String schemaRequest = Ledger.buildSchemaRequest(issuerDid, schemaResult.getSchemaJson()).get();
		signAndSubmitRequest(ctx, issuerWallet, issuerDid, schemaRequest);
	}
	
	void createTranscriptCredentialDefinition(Context ctx) throws Exception {
		
		// Credential Definition Setup
		//
		// References the schema that we just added, and announces who is going to be issuing credentials with that schema,
		// what type of signature method they plan to use (“CL” = “Camenisch Lysyanskya”, the default method used for zero-knowledge proofs by indy),
		// how they plan to handle revocation, and so forth.
		
		// Faber Credential Definition Setup
		
		String getSchemaRequest = Ledger.buildGetSchemaRequest(ctx.faberDid, ctx.faberTranscriptSchemaId).get();
		String getSchemaResponse = Ledger.submitRequest(ctx.pool, getSchemaRequest).get();
		ParseResponseResult parseSchemaResult = Ledger.parseGetSchemaResponse(getSchemaResponse).get();
		log.info(getSchemaResponse);
		
		String configJson = new JSONObject().put("support_revocation", false).toString();
		IssuerCreateAndStoreCredentialDefResult createCredDefResult = Anoncreds.issuerCreateAndStoreCredentialDef(ctx.faberWallet, ctx.faberDid, parseSchemaResult.getObjectJson(), "TAG1", null, configJson).get();
		String transcriptCredDefId = createCredDefResult.getCredDefId();

		String credDefRequest = Ledger.buildCredDefRequest(ctx.faberDid, createCredDefResult.getCredDefJson()).get();
		signAndSubmitRequest(ctx, ctx.faberWallet, ctx.faberDid, credDefRequest);
		
		String getCredDefRequest = Ledger.buildGetCredDefRequest(ctx.faberDid, transcriptCredDefId).get();
		String transcriptGetCredDefResponse = Ledger.submitRequest(ctx.pool, getCredDefRequest).get();
		log.info(transcriptGetCredDefResponse);
	}
	
	void createJobCertificateCredentialDefinition(Context ctx) throws Exception {
		
		// Acme Credential Definition Setup

		String getSchemaRequest = Ledger.buildGetSchemaRequest(ctx.acmeDid, ctx.acmeJobCertificateSchemaId).get();
		String getSchemaResponse = Ledger.submitRequest(ctx.pool, getSchemaRequest).get();
		log.info(getSchemaResponse);
		
		String configJson = new JSONObject().put("support_revocation", false).toString();
		ParseResponseResult parseSchemaResult = Ledger.parseGetSchemaResponse(getSchemaResponse).get();
		IssuerCreateAndStoreCredentialDefResult createCredDefResult = Anoncreds.issuerCreateAndStoreCredentialDef(ctx.acmeWallet, ctx.acmeDid, parseSchemaResult.getObjectJson(), "TAG1", null, configJson).get();
		String jobCertificateCredDefId = createCredDefResult.getCredDefId();

		String credDefRequest = Ledger.buildCredDefRequest(ctx.acmeDid, createCredDefResult.getCredDefJson()).get();
		signAndSubmitRequest(ctx, ctx.acmeWallet, ctx.acmeDid, credDefRequest);
		
		String getCredDefRequest = Ledger.buildGetCredDefRequest(ctx.acmeDid, jobCertificateCredDefId).get();
		String jobCertificateGetCredDefResponse = Ledger.submitRequest(ctx.pool, getCredDefRequest).get();
		log.info(jobCertificateGetCredDefResponse);
	}
	
	private String signAndSubmitRequest(Context ctx, Wallet endorserWallet, String endorserDid, String req) throws Exception {
		String res = Ledger.signAndSubmitRequest(ctx.pool, endorserWallet, endorserDid, req).get();
		if ("REPLY".equals(new JSONObject(res).get("op"))) {
			log.info("SubmitRequest: " + req);
			log.info("SubmitResponse: " + res);
		} else {
			log.warn("SubmitRequest: " + req);
			log.warn("SubmitResponse: " + res);
		}
		return res.toString();
	}

	void closeAndDeletePoolLedger(Context ctx) throws Exception {
		
		log.info("Close Wallets");
		
		closeAndDeleteWallet(ctx.thriftWallet, ctx.thriftWalletConfig, ctx.thriftWalletKey);
		closeAndDeleteWallet(ctx.acmeWallet, ctx.acmeWalletConfig, ctx.acmeWalletKey);
		closeAndDeleteWallet(ctx.faberWallet, ctx.faberWalletConfig, ctx.faberWalletKey);
		closeAndDeleteWallet(ctx.governmentWallet, ctx.governmentWalletConfig, ctx.governmentWalletKey);
		closeAndDeleteWallet(ctx.trusteeWallet, ctx.trusteeWalletConfig, ctx.trusteeWalletKey);
		
		log.info("Close and Delete Pool Ledger");

		ctx.pool.closePoolLedger().get();
		Pool.deletePoolLedgerConfig(ctx.poolName).get();
	}

	void closeAndDeleteWallet(Wallet wallet, String config, String key) throws Exception {
		if (wallet != null) {
			wallet.closeWallet().get();
			Wallet.deleteWallet(config, key).get();
		}
	}
}
