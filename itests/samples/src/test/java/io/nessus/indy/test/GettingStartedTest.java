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

import org.hyperledger.indy.sdk.did.Did;
import org.hyperledger.indy.sdk.did.DidResults.CreateAndStoreMyDidResult;
import org.hyperledger.indy.sdk.ledger.Ledger;
import org.hyperledger.indy.sdk.pool.Pool;
import org.hyperledger.indy.sdk.wallet.Wallet;
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
		String wconfigSteward = new JSONObject().put("id", "Steward").toString();
		String wkeySteward = new JSONObject().put("key", "steward_wallet_key").toString();
		Wallet.createWallet(wconfigSteward, wkeySteward).get();
		
		logInfo("Create wallet - Government");
		String wconfigGovernment = new JSONObject().put("id", "Government").toString();
		String wkeyGovernment = new JSONObject().put("key", "government_wallet_key").toString();
		Wallet.createWallet(wconfigGovernment, wkeyGovernment).get();
		
		logInfo("Create wallet - Faber");
		String wconfigFaber = new JSONObject().put("id", "Faber").toString();
		String wkeyFaber = new JSONObject().put("key", "faber_wallet_key").toString();
		Wallet.createWallet(wconfigFaber, wkeyFaber).get();
		
		logInfo("Create wallet - Acme");
		String wconfigAcme = new JSONObject().put("id", "Acme").toString();
		String wkeyAcme = new JSONObject().put("key", "acme_wallet_key").toString();
		Wallet.createWallet(wconfigAcme, wkeyAcme).get();
		
		logInfo("Create wallet - Thrift");
		String wconfigThrift = new JSONObject().put("id", "Thrift").toString();
		String wkeyThrift = new JSONObject().put("key", "thrift_wallet_key").toString();
		Wallet.createWallet(wconfigThrift, wkeyThrift).get();
		
		// 3. Getting Credential for Steward
		
		logInfo("Open wallet - Steward");
		Wallet walletSteward = Wallet.openWallet(wconfigSteward, wkeySteward).get();
		String seedSteward = new JSONObject().put("seed", "000000000000000000000000Steward1").toString();
		CreateAndStoreMyDidResult didResultSteward = Did.createAndStoreMyDid(walletSteward, seedSteward).get();
		String didSteward = didResultSteward.getDid();
		String vkeySteward = didResultSteward.getVerkey();
		logInfo("DID Steward: did={}, vkey={}", didSteward, vkeySteward);
		Ledger.buildNymRequest(didSteward, didSteward, vkeySteward, null, ROLE_STEWARD).get();

		String res = Ledger.buildGetNymRequest(null, didSteward).get();
		logInfo(res);
		
		// 4. Getting Credentials for Government, Faber, Acme, Thrift
		
		Wallet walletGovernment = Wallet.openWallet(wconfigGovernment, wkeyGovernment).get();
		String seedGovernment = new JSONObject().put("seed", "000000000000000000000Government1").toString();
		CreateAndStoreMyDidResult didResultGovernment = Did.createAndStoreMyDid(walletGovernment, seedGovernment).get();
		String didGovernment = didResultGovernment.getDid();
		String vkeyGovernment = didResultGovernment.getVerkey();
		logInfo("DID Government: did={}, vkey={}", didGovernment, vkeyGovernment);
		Ledger.buildNymRequest(didSteward, didGovernment, vkeyGovernment, null, ROLE_ENDORSER).get();

		Wallet walletFaber = Wallet.openWallet(wconfigFaber, wkeyFaber).get();
		String seedFaber = new JSONObject().put("seed", "00000000000000000000000000Faber1").toString();
		CreateAndStoreMyDidResult didResultFaber = Did.createAndStoreMyDid(walletFaber, seedFaber).get();
		String didFaber = didResultFaber.getDid();
		String vkeyFaber = didResultFaber.getVerkey();
		logInfo("DID Faber: did={}, vkey={}", didFaber, vkeyFaber);
		Ledger.buildNymRequest(didSteward, didFaber, vkeyFaber, null, ROLE_ENDORSER).get();

		Wallet walletAcme = Wallet.openWallet(wconfigAcme, wkeyAcme).get();
		String seedAcme = new JSONObject().put("seed", "000000000000000000000000000Acme1").toString();
		CreateAndStoreMyDidResult didResultAcme = Did.createAndStoreMyDid(walletAcme, seedAcme).get();
		String didAcme = didResultAcme.getDid();
		String vkeyAcme = didResultAcme.getVerkey();
		logInfo("DID Acme: did={}, vkey={}", didAcme, vkeyAcme);
		Ledger.buildNymRequest(didSteward, didAcme, vkeyAcme, null, ROLE_ENDORSER).get();

		Wallet walletThrift = Wallet.openWallet(wconfigThrift, wkeyThrift).get();
		String seedThrift = new JSONObject().put("seed", "0000000000000000000000000Thrift1").toString();
		CreateAndStoreMyDidResult didResultThrift = Did.createAndStoreMyDid(walletThrift, seedThrift).get();
		String didThrift = didResultThrift.getDid();
		String vkeyThrift = didResultThrift.getVerkey();
		logInfo("DID Thrift: did={}, vkey={}", didThrift, vkeyThrift);
		Ledger.buildNymRequest(didSteward, didThrift, vkeyThrift, null, ROLE_ENDORSER).get();

		// 5. Credential Schemas Setup
		//
		// Schemas in indy are very simple JSON documents that specify their name and version, and that list attributes that will appear in a credential.
		// Today, they do not describe data type, recurrence rules, nesting, and other elaborate constructs.

//		String jsonJobCertificate = new JSONObject()
//			.put("name", "Job-Certificate").put("version", "0.2")
//			.put("attr_names", Arrays.asList("first_name","last_name","salary","employee_status","experience"))
//			.toString();
//		logInfo(jsonJobCertificate);
		
		String name = "gvt";
		String version = "1.0";
		String attributes = "[\"age\", \"sex\", \"height\", \"name\"]";
		String schemaDataJSON = "{\"name\":\"" + name + "\",\"version\":\"" + version + "\",\"attr_names\":" + attributes + "}";
		logInfo("DID: " + didSteward + ", Schema: " + schemaDataJSON);
//		String string = Ledger.buildSchemaRequest(didSteward, schemaDataJSON).get();
//		logInfo(string);
		
		// ...
		
		// 9. Close and delete the Wallets
		walletThrift.closeWallet().get();
		walletAcme.closeWallet().get();
		walletFaber.closeWallet().get();
		walletGovernment.closeWallet().get();
		walletSteward.closeWallet().get();

		Wallet.deleteWallet(wconfigThrift, wkeyThrift).get();
		Wallet.deleteWallet(wconfigAcme, wkeyAcme).get();
		Wallet.deleteWallet(wconfigFaber, wkeyFaber).get();
		Wallet.deleteWallet(wconfigGovernment, wkeyGovernment).get();
		Wallet.deleteWallet(wconfigSteward, wkeySteward).get();
		
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
