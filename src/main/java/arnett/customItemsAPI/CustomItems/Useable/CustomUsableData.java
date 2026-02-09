package arnett.customItemsAPI.CustomItems.Useable;

import arnett.customItemsAPI.CustomItems.CustomItemData;

public abstract class CustomUsableData extends CustomItemData {

    protected UsableReceiver receiver;

    public final UsableReceiver getInteractableReceiver()
    {
        if(receiver != null)
            return receiver;

        receiver = createUsableReceiver();
        return receiver;
    }

    protected abstract UsableReceiver createUsableReceiver();
}
