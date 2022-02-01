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

import static io.nessus.indy.utils.IndyConstants.ROLE_STEWARD;

import java.util.Arrays;

import org.hyperledger.indy.sdk.anoncreds.Anoncreds;
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
 */
public class SchemaTest {

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

		// Credential Schemas
		String jobCertificateSchemaId;
		String transcriptSchemaId;
	}
	
	@Test
	public void testWorkflow() throws Exception {
		
		Context ctx = new Context();
		
		// Setup Indy Pool Nodes
		
		logInfo("Create and Open Pool Ledger");
		logInfo("LD_LIBRARY_PATH: {}", System.getenv("LD_LIBRARY_PATH"));
		
		// Set protocol version 2
		Pool.setProtocolVersion(IndyConstants.PROTOCOL_VERSION).get();
		
		// Create ledger config from genesis txn file
		
		logInfo("Create and open Ledger");
		ctx.poolName = PoolUtils.createPoolLedgerConfig("pool1");
		ctx.pool = Pool.openPoolLedger(ctx.poolName, "{}").get();
		
		// Creating Steward Wallet and DID
		
		// Create Wallet for Steward
		
		logInfo("Create wallet - Steward");
		ctx.stewardWalletConfig = new JSONObject().put("id", "Steward").toString();
		ctx.stewardWalletKey = new JSONObject().put("key", "steward_wallet_key").toString();
		Wallet.createWallet(ctx.stewardWalletConfig, ctx.stewardWalletKey).get();
		ctx.stewardWallet = Wallet.openWallet(ctx.stewardWalletConfig, ctx.stewardWalletKey).get();
		
		// Getting Credential for Steward
		
		String stewardSeed = new JSONObject().put("seed", "000000000000000000000000Steward1").toString();
		CreateAndStoreMyDidResult didResult = Did.createAndStoreMyDid(ctx.stewardWallet, stewardSeed).get();
		ctx.stewardDid = didResult.getDid();
		ctx.stewardVkey = didResult.getVerkey();
		
		logInfo("DID Steward: did={}, vkey={}", ctx.stewardDid, ctx.stewardVkey);
		String nymRequest = Ledger.buildNymRequest(ctx.stewardDid, ctx.stewardDid, ctx.stewardVkey, null, ROLE_STEWARD).get();
		Ledger.signAndSubmitRequest(ctx.pool, ctx.stewardWallet, ctx.stewardDid, nymRequest).get();

		// Creating Credential Schemas
		
		String issuerDid = ctx.stewardDid;
		Wallet issuerWallet = ctx.stewardWallet;

		IssuerCreateSchemaResult schemaResult = Anoncreds.issuerCreateSchema(issuerDid, "Transcript", "1.2", new JSONArray(Arrays.asList("first_name","last_name","degree","status","year","average","ssn")).toString()).get();		
		logInfo("IssuerCreateSchemaResult: " + schemaResult);
		Ledger.buildSchemaRequest(issuerDid, schemaResult.getSchemaJson()).get();
		ctx.transcriptSchemaId = schemaResult.getSchemaId();
		
		String schemaRequest = Ledger.buildSchemaRequest(issuerDid, schemaResult.getSchemaJson()).get();
		Ledger.signAndSubmitRequest(ctx.pool, issuerWallet, issuerDid, schemaRequest).get();

		String getSchemaRequest = Ledger.buildGetSchemaRequest(ctx.faberDid, ctx.transcriptSchemaId).get();
		logInfo("GetSchemaRequest: " + getSchemaRequest);
		String getSchemaResponse = Ledger.submitRequest(ctx.pool, getSchemaRequest).get();
		logInfo("GetSchemaRespone: " + getSchemaResponse);
		ParseResponseResult parseSchemaResult = Ledger.parseGetSchemaResponse(getSchemaResponse).get();
		logInfo("ParseResponseResult: " + parseSchemaResult);
		
		// Close and Delete Indy Pool Nodes
		
		closeAndDeleteWallet(ctx.stewardWallet, ctx.stewardWalletConfig, ctx.stewardWalletKey);
		
		logInfo("Close and Delete Pool Ledger");

		ctx.pool.closePoolLedger().get();
		Pool.deletePoolLedgerConfig(ctx.poolName).get();
	}

	private void closeAndDeleteWallet(Wallet wallet, String config, String key) throws Exception {
		if (wallet != null) {
			wallet.closeWallet().get();
			Wallet.deleteWallet(config, key).get();
		}
	}
	
	private void logInfo(String msg, Object... args) {
		log.info(msg, args);
	}
}
