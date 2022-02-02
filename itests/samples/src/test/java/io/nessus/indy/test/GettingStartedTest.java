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
import static org.junit.Assert.assertEquals;

import java.util.Arrays;

import org.hyperledger.indy.sdk.anoncreds.Anoncreds;
import org.hyperledger.indy.sdk.anoncreds.AnoncredsResults.IssuerCreateAndStoreCredentialDefResult;
import org.hyperledger.indy.sdk.anoncreds.AnoncredsResults.IssuerCreateCredentialResult;
import org.hyperledger.indy.sdk.anoncreds.AnoncredsResults.IssuerCreateSchemaResult;
import org.hyperledger.indy.sdk.anoncreds.AnoncredsResults.ProverCreateCredentialRequestResult;
import org.hyperledger.indy.sdk.anoncreds.CredentialsSearchForProofReq;
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
		String transcriptSchemaId;
		String transcriptCredDefId;
		
		// Acme
		String acmeWalletConfig;
		String acmeWalletKey;
		Wallet acmeWallet;
		String acmeDid;
		String acmeVkey;
		String jobCertificateSchemaId;
		String jobCertificateCredDefId;
		
		// Thrift
		String thriftWalletConfig;
		String thriftWalletKey;
		Wallet thriftWallet;
		String thriftDid;
		String thriftVkey;
		
		// Alice
		String aliceWalletConfig;
		String aliceWalletKey;
		Wallet aliceWallet;
		String aliceDid;
		String aliceVkey;
		String aliceMasterSecretId;
	}
	
	@Test
	public void testWorkflow() throws Exception {
		
		Context ctx = new Context();
		
		// Setup Indy Pool Nodes
		
		createAndOpenPoolLedger(ctx);
		
		// Creating Trustee Wallet and DID
		
		createTrustee(ctx);
		
		// Onboarding Government, Faber, Acme, Thrift 
		
		onboardGovernment(ctx);
		onboardFaberCollege(ctx);
		onboardAcmeCorp(ctx);
		onboardThriftBank(ctx);
		onboardAlice(ctx);
		
		// Creating Credential Schemas
		
		createTranscriptSchema(ctx);
		createJobCertificateSchema(ctx);
		
		// Creating Credential Definitions
		
		createTranscriptCredentialDefinition(ctx);
		createJobCertificateCredentialDefinition(ctx);
		
		// Alice gets her Transcript from Faber College
		
		getTranscriptFromFaber(ctx);
		
		// Alice applies for a job with Acme
		
		applyForJobWithAcme(ctx);
		
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
		ctx.acmeWalletKey = new JSONObject().put("key", "acme_wallet_key").toString();
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
		ctx.thriftWalletKey = new JSONObject().put("key", "thrift_wallet_key").toString();
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
	
	void onboardAlice(Context ctx) throws Exception {
		
		// Create Wallet for Alice
		
		log.info("Create wallet - Alice");
		ctx.aliceWalletConfig = new JSONObject().put("id", "Alice").toString();
		ctx.aliceWalletKey = new JSONObject().put("key", "alice").toString();
		Wallet.createWallet(ctx.aliceWalletConfig, ctx.aliceWalletKey).get();
		ctx.aliceWallet = Wallet.openWallet(ctx.aliceWalletConfig, ctx.aliceWalletKey).get();
		
		// Getting Credential for Alice
		
		String aliceSeed = new JSONObject().put("seed", "00000000000000000000000000Alice1").toString();
		CreateAndStoreMyDidResult didResult = Did.createAndStoreMyDid(ctx.aliceWallet, aliceSeed).get();
		ctx.aliceDid = didResult.getDid();
		ctx.aliceVkey = didResult.getVerkey();
		
		log.info("DID Alice: did={}, vkey={}", ctx.aliceDid, ctx.aliceVkey);
		String nymRequest = Ledger.buildNymRequest(ctx.governmentDid, ctx.aliceDid, ctx.aliceVkey, null, null).get();
		signAndSubmitRequest(ctx, ctx.governmentWallet, ctx.governmentDid, nymRequest);
	}
	
	void createTranscriptSchema(Context ctx) throws Exception {
		
		// Schemas in indy are very simple JSON documents that specify their name and version, and that list attributes that will appear in a credential.
		// Today, they do not describe data type, recurrence rules, nesting, and other elaborate constructs.
		
		Wallet issuerWallet = ctx.faberWallet;
		String issuerDid = ctx.faberDid;
		
		IssuerCreateSchemaResult schemaResult = Anoncreds.issuerCreateSchema(issuerDid, "Transcript", "1.2", 
				new JSONArray(Arrays.asList("first_name","last_name","degree","status","year","average","ssn")).toString()).get();
		
		log.info(schemaResult.toString());
		Ledger.buildSchemaRequest(issuerDid, schemaResult.getSchemaJson()).get();
		ctx.transcriptSchemaId = schemaResult.getSchemaId();
		
		String schemaRequest = Ledger.buildSchemaRequest(issuerDid, schemaResult.getSchemaJson()).get();
		signAndSubmitRequest(ctx, issuerWallet, issuerDid, schemaRequest);
	}
	
	void createJobCertificateSchema(Context ctx) throws Exception {
		
		Wallet issuerWallet = ctx.acmeWallet;
		String issuerDid = ctx.acmeDid;
		
		IssuerCreateSchemaResult schemaResult = Anoncreds.issuerCreateSchema(issuerDid, "Job-Certificate", "0.2", 
				new JSONArray(Arrays.asList("first_name","last_name","salary","employee_status","experience")).toString()).get();
		
		log.info(schemaResult.toString());
		Ledger.buildSchemaRequest(issuerDid, schemaResult.getSchemaJson()).get();
		ctx.jobCertificateSchemaId = schemaResult.getSchemaId();
		
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
		
		String getSchemaRequest = Ledger.buildGetSchemaRequest(ctx.faberDid, ctx.transcriptSchemaId).get();
		String getSchemaResponse = Ledger.submitRequest(ctx.pool, getSchemaRequest).get();
		ParseResponseResult parseSchemaResult = Ledger.parseGetSchemaResponse(getSchemaResponse).get();
		log.info(getSchemaResponse);
		
		String configJson = new JSONObject().put("support_revocation", false).toString();
		IssuerCreateAndStoreCredentialDefResult createCredDefResult = Anoncreds.issuerCreateAndStoreCredentialDef(ctx.faberWallet, ctx.faberDid, parseSchemaResult.getObjectJson(), "TAG1", null, configJson).get();
		ctx.transcriptCredDefId = createCredDefResult.getCredDefId();

		String credDefRequest = Ledger.buildCredDefRequest(ctx.faberDid, createCredDefResult.getCredDefJson()).get();
		signAndSubmitRequest(ctx, ctx.faberWallet, ctx.faberDid, credDefRequest);

		String getCredDefRequest = Ledger.buildGetCredDefRequest(ctx.faberDid, ctx.transcriptCredDefId).get();
		String getCredDefResponse = Ledger.submitRequest(ctx.pool, getCredDefRequest).get();
		log.info(getCredDefResponse);
	}
	
	void createJobCertificateCredentialDefinition(Context ctx) throws Exception {
		
		// Acme Credential Definition Setup

		String getSchemaRequest = Ledger.buildGetSchemaRequest(ctx.acmeDid, ctx.jobCertificateSchemaId).get();
		String getSchemaResponse = Ledger.submitRequest(ctx.pool, getSchemaRequest).get();
		log.info(getSchemaResponse);
		
		String configJson = new JSONObject().put("support_revocation", false).toString();
		ParseResponseResult parseSchemaResult = Ledger.parseGetSchemaResponse(getSchemaResponse).get();
		IssuerCreateAndStoreCredentialDefResult createCredDefResult = Anoncreds.issuerCreateAndStoreCredentialDef(ctx.acmeWallet, ctx.acmeDid, parseSchemaResult.getObjectJson(), "TAG1", null, configJson).get();
		ctx.jobCertificateCredDefId = createCredDefResult.getCredDefId();

		String credDefRequest = Ledger.buildCredDefRequest(ctx.acmeDid, createCredDefResult.getCredDefJson()).get();
		signAndSubmitRequest(ctx, ctx.acmeWallet, ctx.acmeDid, credDefRequest);

		String getCredDefRequest = Ledger.buildGetCredDefRequest(ctx.acmeDid, ctx.jobCertificateCredDefId).get();
		String getCredDefResponse = Ledger.submitRequest(ctx.pool, getCredDefRequest).get();
		log.info(getCredDefResponse);
	}
	
	void getTranscriptFromFaber(Context ctx) throws Exception {
		
		// Faber creates a Transcript Credential Offer for Alice
		
		String transcriptCredOffer = Anoncreds.issuerCreateCredentialOffer(ctx.faberWallet, ctx.transcriptCredDefId).get();
		String transcriptCredDefId = new JSONObject(transcriptCredOffer).getString("cred_def_id");
		
		// Alice creates a Master Secret in her Wallet
		
		ctx.aliceMasterSecretId = Anoncreds.proverCreateMasterSecret(ctx.aliceWallet, null).get();
		
		// Alice gets Credential Definition from Ledger
		
		// [TODO] Use pairwise DID Alice => Faber
		String credDefResponse = submitRequest(ctx, Ledger.buildGetCredDefRequest(ctx.aliceDid, transcriptCredDefId).get());
		String transcriptCredDef = Ledger.parseGetCredDefResponse(credDefResponse).get().getObjectJson();
				
		// Alice creates Transcript Credential Request for Faber
		
		ProverCreateCredentialRequestResult credentialRequestResult = Anoncreds.proverCreateCredentialReq(ctx.aliceWallet, ctx.aliceDid, transcriptCredOffer, transcriptCredDef, ctx.aliceMasterSecretId).get();
		String credentialRequestJson = credentialRequestResult.getCredentialRequestJson();
		String credentialRequestMetadataJson = credentialRequestResult.getCredentialRequestMetadataJson();
		
		// Faber creates Transcript Credential for Alice
		
		String credValuesJson = new JSONObject()
			.put("first_name", new JSONObject().put("raw", "Alice").put("encoded", "1139481716457488690172217916278103335"))
			.put("last_name", new JSONObject().put("raw", "Garcia").put("encoded", "5321642780241790123587902456789123452"))
			.put("degree", new JSONObject().put("raw", "Bachelor of Science, Marketing").put("encoded", "12434523576212321"))
			.put("status", new JSONObject().put("raw", "graduated").put("encoded", "2213454313412354"))
			.put("ssn", new JSONObject().put("raw", "123-45-6789").put("encoded", "3124141231422543541"))
			.put("year", new JSONObject().put("raw", "2015").put("encoded", "2015"))
			.put("average", new JSONObject().put("raw", "5").put("encoded", "5")).toString();
		
		IssuerCreateCredentialResult issuerCredentialResult = Anoncreds.issuerCreateCredential(ctx.faberWallet, transcriptCredOffer, credentialRequestJson, credValuesJson, null, 0).get();
		String transcriptCredJson = issuerCredentialResult.getCredentialJson();
		log.info("IssuedCredential: " + transcriptCredJson);
		
		// Alice stores Transcript Credential from Faber
		
		String transcriptCredentialId = Anoncreds.proverStoreCredential(ctx.aliceWallet, null, credentialRequestMetadataJson, transcriptCredJson, transcriptCredDef, null).get();
		log.info("Transcript CredentialId: " + transcriptCredentialId);
	}
	
	void applyForJobWithAcme(Context ctx) throws Exception {
		
		// Acme creates a Job Application Proof Request
		
		String nonce = Anoncreds.generateNonce().get();
		JSONArray credDefRestrictions = new JSONArray().put(new JSONObject().put("cred_def_id", ctx.transcriptCredDefId));
		
		String proofRequestJson = new JSONObject()
			.put("nonce", nonce)
			.put("name", "Job-Application")
			.put("version", "0.1")
			.put("requested_attributes", new JSONObject()
				.put("attr1_referent", new JSONObject().put("name", "first_name"))
				.put("attr2_referent", new JSONObject().put("name", "last_name"))
				.put("attr3_referent", new JSONObject().put("name", "degree").put("restrictions", credDefRestrictions))
				.put("attr4_referent", new JSONObject().put("name", "status").put("restrictions", credDefRestrictions))
				.put("attr5_referent", new JSONObject().put("name", "ssn").put("restrictions", credDefRestrictions))
				.put("attr6_referent", new JSONObject().put("name", "year").put("restrictions", credDefRestrictions))
			)
			.put("requested_predicates", new JSONObject()
				.put("predicate1_referent", new JSONObject()
					.put("name", "average")
					.put("p_type", ">=")
					.put("p_value", 4)
					.put("restrictions", credDefRestrictions)
					)
			).toString();
		log.info("JobApplication Proof Request: " + proofRequestJson);
		
		// Alice gets Credentials for Job Application Proof Request
		
		CredentialsSearchForProofReq credentialsSearch = CredentialsSearchForProofReq.open(ctx.aliceWallet, proofRequestJson, null).get();
		
		JSONArray credentialsForAttribute3 = new JSONArray(credentialsSearch.fetchNextCredentials("attr3_referent", 100).get());
		String credentialIdForAttribute3 = credentialsForAttribute3.getJSONObject(0).getJSONObject("cred_info").getString("referent");

		JSONArray credentialsForAttribute4 = new JSONArray(credentialsSearch.fetchNextCredentials("attr4_referent", 100).get());
		String credentialIdForAttribute4 = credentialsForAttribute4.getJSONObject(0).getJSONObject("cred_info").getString("referent");

		JSONArray credentialsForAttribute5 = new JSONArray(credentialsSearch.fetchNextCredentials("attr5_referent", 100).get());
		String credentialIdForAttribute5 = credentialsForAttribute5.getJSONObject(0).getJSONObject("cred_info").getString("referent");

		JSONArray credentialsForAttribute6 = new JSONArray(credentialsSearch.fetchNextCredentials("attr6_referent", 100).get());
		String credentialIdForAttribute6 = credentialsForAttribute6.getJSONObject(0).getJSONObject("cred_info").getString("referent");

		JSONArray credentialsForPredicate1 = new JSONArray(credentialsSearch.fetchNextCredentials("predicate1_referent", 100).get());
		String credentialIdForPredicate1 = credentialsForPredicate1.getJSONObject(0).getJSONObject("cred_info").getString("referent");
		
		credentialsSearch.close();
		
		// Alice provides Job Application Proof
		
		String requestedCredentialsJson = new JSONObject()
			.put("self_attested_attributes", new JSONObject()
					.put("attr1_referent", "Alice")
					.put("attr2_referent", "Garcia"))
			.put("requested_attributes", new JSONObject()
				.put("attr3_referent", new JSONObject()
					.put("cred_id", credentialIdForAttribute3)
					.put("revealed", true))
				.put("attr4_referent", new JSONObject()
					.put("cred_id", credentialIdForAttribute4)
					.put("revealed", true))
				.put("attr5_referent", new JSONObject()
					.put("cred_id", credentialIdForAttribute5)
					.put("revealed", true))
				.put("attr6_referent", new JSONObject()
					.put("cred_id", credentialIdForAttribute6)
					.put("revealed", true)))
			.put("requested_predicates", new JSONObject()
					.put("predicate1_referent", new JSONObject()
						.put("cred_id",credentialIdForPredicate1)))
			.toString();
		
		String getSchemaRequest = Ledger.buildGetSchemaRequest(ctx.faberDid, ctx.transcriptSchemaId).get();
		String getSchemaResponse = Ledger.submitRequest(ctx.pool, getSchemaRequest).get();
		ParseResponseResult parseSchemaResult = Ledger.parseGetSchemaResponse(getSchemaResponse).get();
		String schemaJson = parseSchemaResult.getObjectJson();
		
		String getCredDefRequest = Ledger.buildGetCredDefRequest(ctx.faberDid, ctx.transcriptCredDefId).get();
		String getCredDefResponse = Ledger.submitRequest(ctx.pool, getCredDefRequest).get();
		ParseResponseResult parseCredDefResponse = Ledger.parseGetCredDefResponse(getCredDefResponse).get();
		String credDefJson = parseCredDefResponse.getObjectJson();
		
		String schemas = new JSONObject().put(ctx.transcriptSchemaId, new JSONObject(schemaJson)).toString();
		String credDefs = new JSONObject().put(ctx.transcriptCredDefId,  new JSONObject(credDefJson)).toString();

		String proofJson = Anoncreds.proverCreateProof(ctx.aliceWallet, proofRequestJson, requestedCredentialsJson, ctx.aliceMasterSecretId, schemas, credDefs, "{}").get();
		JSONObject selfAttestedAttrs = new JSONObject(proofJson).getJSONObject("requested_proof").getJSONObject("self_attested_attrs");
		JSONObject revealedAttrs = new JSONObject(proofJson).getJSONObject("requested_proof").getJSONObject("revealed_attrs");
		log.info("SelfAttestedAttrs: " + selfAttestedAttrs);
		log.info("RevealedAttrs: " + revealedAttrs);
		
		// Acme verifies Job Application Proof for Alice
		
		assertEquals("Alice", selfAttestedAttrs.getString("attr1_referent"));
		assertEquals("Garcia", selfAttestedAttrs.getString("attr2_referent"));
		assertEquals("Bachelor of Science, Marketing", revealedAttrs.getJSONObject("attr3_referent").getString("raw"));
		assertEquals("graduated", revealedAttrs.getJSONObject("attr4_referent").getString("raw"));
		assertEquals("123-45-6789", revealedAttrs.getJSONObject("attr5_referent").getString("raw"));
		assertEquals("2015", revealedAttrs.getJSONObject("attr6_referent").getString("raw"));
	}
	
	private String signAndSubmitRequest(Context ctx, Wallet endorserWallet, String endorserDid, String request) throws Exception {
		return submitRequest(ctx, Ledger.signRequest(endorserWallet, endorserDid, request).get());
	}

	private String submitRequest(Context ctx, String req) throws Exception {
		String res = Ledger.submitRequest(ctx.pool, req).get();
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
		
		closeAndDeleteWallet(ctx.aliceWallet, ctx.aliceWalletConfig, ctx.aliceWalletKey);
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
