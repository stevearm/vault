package com.horsefire.vault.couch;

import java.util.Arrays;
import java.util.List;

import junit.framework.TestCase;

import org.junit.Test;

import com.horsefire.vault.couch.VaultDocument.Addressable;

public class SyncServiceTest extends TestCase {

	/**
	 * This is because I always need to check order (descending vs ascending)
	 * experementally
	 */
	@Test
	public void testPrioritySort() {
		VaultDocument docPos2 = new VaultDocument();
		docPos2.addressable = new Addressable();
		docPos2.addressable.priority = 2;

		VaultDocument docPos1 = new VaultDocument();
		docPos1.addressable = new Addressable();
		docPos1.addressable.priority = 1;

		VaultDocument docZero = new VaultDocument();
		docZero.addressable = new Addressable();
		docZero.addressable.priority = 0;

		VaultDocument docNeg2 = new VaultDocument();
		docNeg2.addressable = new Addressable();
		docNeg2.addressable.priority = -2;

		List<VaultDocument> vaults = Arrays.asList(docZero, docPos2, docNeg2,
				docPos1);
		SyncService.sortPriorityDesc(vaults);

		assertSame(docPos2, vaults.get(0));
		assertSame(docPos1, vaults.get(1));
		assertSame(docZero, vaults.get(2));
		assertSame(docNeg2, vaults.get(3));
	}
}
