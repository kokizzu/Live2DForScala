package moe.brianhsu.porting.live2d.framework.exception

/**
 * Denote exception that when called csmReviveMocInPlace, it returns NULL.
 */
class MocNotRevivedException extends Exception("Cannot revived moc file")