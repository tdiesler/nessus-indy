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

package io.nessus.indy.test.samples;

import static io.nessus.indy.utils.IndyConstants.PROTOCOL_VERSION;
import static org.hyperledger.indy.sdk.crypto.Crypto.authCrypt;
import static org.hyperledger.indy.sdk.crypto.Crypto.authDecrypt;
import static org.hyperledger.indy.sdk.did.Did.createAndStoreMyDid;

import java.util.Arrays;

import org.hyperledger.indy.sdk.crypto.CryptoResults.AuthDecryptResult;
import org.hyperledger.indy.sdk.did.DidResults.CreateAndStoreMyDidResult;
import org.hyperledger.indy.sdk.pool.Pool;
import org.hyperledger.indy.sdk.wallet.Wallet;
import org.json.JSONObject;
import org.junit.Assert;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.nessus.indy.utils.PoolUtils;


public class CryptoSampleTest {

	Logger log = LoggerFactory.getLogger(getClass());
	
	@Test
	public void testLedger() throws Exception {
		
		logInfo("Crypto sample -> started");

		// Set protocol version 2 to work with Indy Node 1.4
		Pool.setProtocolVersion(PROTOCOL_VERSION).get();

		// 1. Create and Open Pool
		String poolName = PoolUtils.createPoolLedgerConfig();
		Pool pool = Pool.openPoolLedger(poolName, "{}").get();

		// 2. Create and Open My Wallet
		String myWalletConfig = new JSONObject().put("id", "myWallet").toString();
		String myWalletCredentials = new JSONObject().put("key", "my_wallet_key").toString();
		Wallet.createWallet(myWalletConfig, myWalletCredentials).get();
		Wallet myWallet = Wallet.openWallet(myWalletConfig, myWalletCredentials).get();

		// 3. Create and Open Their Wallet
		String theirWalletConfig = new JSONObject().put("id", "theirWallet").toString();
		String theirWalletCredentials = new JSONObject().put("key", "their_wallet_key").toString();
		Wallet.createWallet(theirWalletConfig, theirWalletCredentials).get();
		Wallet theirWallet = Wallet.openWallet(theirWalletConfig, theirWalletCredentials).get();

		// 4. Create My Did
		CreateAndStoreMyDidResult myDid = createAndStoreMyDid(myWallet, "{}").get();
		String myVerkey = myDid.getVerkey();

		// 5. Create Their Did
		CreateAndStoreMyDidResult createTheirDidResult = createAndStoreMyDid(theirWallet, "{}").get();
		String theirVerkey = createTheirDidResult.getVerkey();

		// 6. Their auth encrypt message
		String msg = new JSONObject()
				.put("reqId", "1495034346617224651")
				.put("identifier", "GJ1SzoWzavQYfNL9XkaJdrQejfztN4XqdsiV4ct3LXKL")
				.put("operation", new JSONObject()
						.put("type", "1")
						.put("dest", "4efZu2SXufS556yss7W5k6Po37jt4371RM4whbPKBKdB")
				)
				.toString();

		byte[] encryptedMessage = authCrypt(theirWallet, theirVerkey, myVerkey, msg.getBytes()).get();

		// 7. I decrypt message
		AuthDecryptResult authDecryptResult = authDecrypt(myWallet, myVerkey, encryptedMessage).get();

		Assert.assertTrue(Arrays.equals(msg.getBytes(), authDecryptResult.getDecryptedMessage()));
		Assert.assertEquals(theirVerkey, authDecryptResult.getVerkey());

		// 8. Close and delete My Wallet
		myWallet.closeWallet().get();
		Wallet.deleteWallet(myWalletConfig, myWalletCredentials).get();

		// 9. Close and delete Their Wallet
		theirWallet.closeWallet().get();
		Wallet.deleteWallet(theirWalletConfig, theirWalletCredentials).get();

		// 10. Close Pool
		pool.closePoolLedger().get();

		// 11. Delete Pool ledger config
		Pool.deletePoolLedgerConfig(poolName).get();

		logInfo("Crypto sample -> completed");
	}

	private void logInfo(String msg, Object... args) {
		log.info(msg, args);
	}
}
