package arnett.customItemsAPI.Helpers;

import net.minecraft.ChatFormatting;

public class TextColorHelper {
    public static ChatFormatting getTextFormatColor(String color)
    {
        try {
            //unfortunately, these colors don't match the ChatFormatting colors so we gotta do some mismatching
            return switch (color.toUpperCase())
            {
                case "LIGHT_GRAY" -> ChatFormatting.GRAY;
                case "GRAY" -> ChatFormatting.DARK_GRAY;
                case "BLACK" -> ChatFormatting.BLACK;
                case "BROWN" -> ChatFormatting.GOLD;
                case "RED" -> ChatFormatting.RED;
                case "ORANGE" -> ChatFormatting.GOLD;
                case "YELLOW" -> ChatFormatting.YELLOW;
                case "LIME" -> ChatFormatting.GREEN;
                case "GREEN" -> ChatFormatting.GREEN;
                case "CYAN" -> ChatFormatting.AQUA;
                case "LIGHT_BLUE" -> ChatFormatting.BLUE;
                case "BLUE" -> ChatFormatting.DARK_BLUE;
                case "PURPLE" -> ChatFormatting.DARK_PURPLE;
                case "MAGENTA" -> ChatFormatting.LIGHT_PURPLE;
                case "PINK" -> ChatFormatting.LIGHT_PURPLE;
                default -> ChatFormatting.WHITE;
            };
        }
        catch (Exception e)
        {
            return ChatFormatting.WHITE;
        }
    }
}
