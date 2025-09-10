package PTI.Rs232Validator;

public enum Rs232State {
    /// <summary>
    ///     Validator is not in any state
    /// </summary>
    /// <remarks>On power-up, a device may be in the None state</remarks>
    None,

    /// <summary>
    ///     Device is idle, ready to accept bills
    /// </summary>
    Idling,

    /// <summary>
    ///     Device feeding bill in preparation for validation
    /// </summary>
    Accepting,

    /// <summary>
    ///     A bill is held in escrow awaiting host instruction
    /// </summary>
    /// <remarks>Only used in escrow mode. Instructions are Stack or Return</remarks>
    Escrowed,

    /// <summary>
    ///     A bill is being stacked in the cashbox
    /// </summary>
    Stacking,

    /// <summary>
    ///     A bill is being returned to the customer
    /// </summary>
    Returning,

    /// <summary>
    ///     The bill validator is jammed
    /// </summary>
    BillJammed,

    /// <summary>
    ///     The cashbox is full
    /// </summary>
    StackerFull,

    /// <summary>
    ///     The bill validator has failed and cannot recover itself
    /// </summary>
    Failure;

    private Rs232State() {}

}
