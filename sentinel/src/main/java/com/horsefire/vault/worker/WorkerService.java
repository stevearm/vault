package com.horsefire.vault.worker;

/**
 * This class will listen to the worker's database's _changes feed. When a
 * change to the worker's db entry is detected, the entry is loaded, the jar's
 * SHA1 is checked against the one downloaded. If different, it downloads the
 * new one. Regardless, it then checks the triggered timestamp to see if it's
 * after the started timestamp. If so, it runs the worker, and sets started to
 * now. When the worker is finished, it sets finished to now.
 */
public class WorkerService {

}
