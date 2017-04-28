package me.desht.modularrouters.integration.guideapi;

import amerifrance.guideapi.api.GuideAPI;
import amerifrance.guideapi.api.GuideBook;
import amerifrance.guideapi.api.IGuideBook;
import amerifrance.guideapi.api.IPage;
import amerifrance.guideapi.api.impl.Book;
import amerifrance.guideapi.api.impl.abstraction.CategoryAbstract;
import amerifrance.guideapi.api.impl.abstraction.EntryAbstract;
import amerifrance.guideapi.api.util.PageHelper;
import amerifrance.guideapi.category.CategoryItemStack;
import amerifrance.guideapi.entry.EntryItemStack;
import amerifrance.guideapi.page.PageIRecipe;
import amerifrance.guideapi.page.PageText;
import amerifrance.guideapi.page.reciperenderer.ShapedOreRecipeRenderer;
import com.google.common.collect.Lists;
import me.desht.modularrouters.ModularRouters;
import me.desht.modularrouters.block.ModBlocks;
import me.desht.modularrouters.block.tile.TileEntityItemRouter;
import me.desht.modularrouters.config.ConfigHandler;
import me.desht.modularrouters.item.ModItems;
import me.desht.modularrouters.item.module.ItemModule;
import me.desht.modularrouters.item.module.ItemModule.ModuleType;
import me.desht.modularrouters.item.smartfilter.ItemSmartFilter;
import me.desht.modularrouters.item.smartfilter.ItemSmartFilter.FilterType;
import me.desht.modularrouters.item.upgrade.ItemUpgrade;
import me.desht.modularrouters.item.upgrade.ItemUpgrade.UpgradeType;
import me.desht.modularrouters.recipe.enhancement.*;
import me.desht.modularrouters.util.ModuleHelper;
import net.minecraft.init.Blocks;
import net.minecraft.init.Items;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fml.common.registry.GameRegistry;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import net.minecraftforge.oredict.ShapedOreRecipe;

import javax.annotation.Nullable;
import java.awt.*;
import java.util.*;
import java.util.List;
import java.util.stream.Collectors;

import static me.desht.modularrouters.util.MiscUtil.translate;

@GuideBook
public class Guidebook implements IGuideBook {
    private static final int MAX_PAGE_LENGTH = 270;

    private static Book guideBook;

    @Nullable
    @Override
    public Book buildBook() {
        Map<ResourceLocation, EntryAbstract> entries;
        List<CategoryAbstract> categories = new ArrayList<>();
        List<IPage> pages;

        // Intro category
        entries = new LinkedHashMap<>();
        pages = new ArrayList<>(PageHelper.pagesForLongText(translate("guidebook.para.introText"), MAX_PAGE_LENGTH));
        entries.put(rl("intro"),
                new EntryItemStack(pages, translate("guidebook.words.overview"), new ItemStack(ModBlocks.itemRouter)));
        categories.add(new CategoryItemStack(entries, translate("guidebook.categories.introduction"), new ItemStack(Items.WRITABLE_BOOK)));

        // Routers category
        entries = new LinkedHashMap<>();
        pages = new ArrayList<>(PageHelper.pagesForLongText(translate("guidebook.para.routerText"), MAX_PAGE_LENGTH));
        pages.add(new PageIRecipe(
                        new ShapedOreRecipe(new ItemStack(ModBlocks.itemRouter, 4),
                                "ibi", "bmb", "ibi", 'b', Blocks.IRON_BARS, 'i', Items.IRON_INGOT, 'm', ModItems.blankModule)
                )
        );
        entries.put(rl("router"),
                new EntryItemStack(pages, translate("tile.item_router.name"), new ItemStack(ModBlocks.itemRouter)));
        pages = new ArrayList<>(PageHelper.pagesForLongText(translate("guidebook.para.routerEcoMode", ModuleType.values().length, String.valueOf(ConfigHandler.getConfigKey())), MAX_PAGE_LENGTH));
        entries.put(rl("routerEcoMode"),
                new EntryItemStack(pages, translate("guidebook.words.ecoMode"), new ItemStack(Blocks.SAPLING)));
        categories.add(new CategoryItemStack(entries, translate("guidebook.categories.routers"), new ItemStack(ModBlocks.itemRouter)));

        // Modules category
        entries = new LinkedHashMap<>();
        pages = new ArrayList<>(PageHelper.pagesForLongText(translate("guidebook.para.moduleOverview", ModuleType.values().length, String.valueOf(ConfigHandler.getConfigKey())), MAX_PAGE_LENGTH));
        entries.put(rl("moduleOverview"), new EntryItemStack(pages, translate("guidebook.words.overview"), new ItemStack(Items.BOOK)));
        pages = Arrays.asList(
                new PageText(translate("guidebook.para.blankModule")),
                new PageIRecipe(new ShapedOreRecipe(new ItemStack(ModItems.blankModule, 6),
                        " r ", "ppp", "nnn", 'r', Items.REDSTONE, 'p', Items.PAPER, 'n', Items.GOLD_NUGGET)
                )
        );
        ItemStack bm = new ItemStack(ModItems.blankModule);
        entries.put(rl("blankModule"), new EntryItemStack(pages, translate(bm.getUnlocalizedName() + ".name"), bm));
        buildModulePages(entries);
        categories.add(new CategoryItemStack(entries, translate("guiText.label.modules"), new ItemStack(ModItems.blankModule)));

        // Upgrades category
        entries = new LinkedHashMap<>();
        pages = new ArrayList<>(PageHelper.pagesForLongText(translate("guidebook.para.upgradeOverview", UpgradeType.values().length, TileEntityItemRouter.N_UPGRADE_SLOTS), MAX_PAGE_LENGTH));
        entries.put(rl("upgradeOverview"), new EntryItemStack(pages, translate("guidebook.words.overview"), new ItemStack(Items.BOOK)));
        pages = Arrays.asList(
                new PageText(translate("guidebook.para.blankUpgrade")),
                new PageIRecipe(new ShapedOreRecipe(new ItemStack(ModItems.blankUpgrade, 6),
                        "ppn", "pdn", " pn", 'p', Items.PAPER, 'd', Items.DIAMOND, 'n', Items.GOLD_NUGGET))

        );
        ItemStack bu = new ItemStack(ModItems.blankUpgrade);
        entries.put(rl("blankUpgrade"), new EntryItemStack(pages, translate(bu.getUnlocalizedName() + ".name"), bu));
        buildUpgradePages(entries);
        categories.add(new CategoryItemStack(entries, translate("guiText.label.upgrades"), new ItemStack(ModItems.blankUpgrade)));

        // Enhancements category
        entries = new LinkedHashMap<>();
        pages = new ArrayList<>(PageHelper.pagesForLongText(translate("guidebook.para.enhancementsOverview"), MAX_PAGE_LENGTH));
        entries.put(rl("enhancementsOverview"), new EntryItemStack(pages, translate("guidebook.words.enhancements"), new ItemStack(Items.BOOK)));
        buildEnhancementPages(entries);
        categories.add(new CategoryItemStack(entries, translate("guidebook.words.enhancements"), new ItemStack(Blocks.CRAFTING_TABLE)));

        // Filters category
        entries = new LinkedHashMap<>();
        pages = new ArrayList<>(PageHelper.pagesForLongText(translate("guidebook.para.filterOverview", FilterType.values().length), MAX_PAGE_LENGTH));
        entries.put(rl("filterOverview"), new EntryItemStack(pages, translate("guidebook.words.overview"), new ItemStack(Items.BOOK)));
        buildFilterPages(entries);
        categories.add(new CategoryItemStack(entries, translate("guiText.label.filters"), ItemSmartFilter.makeItemStack(FilterType.BULKITEM)));

        // and done
        guideBook = new Book();
        guideBook.setAuthor("desht");
        guideBook.setTitle("Modular Routers Guide");
        guideBook.setDisplayName("Modular Routers Guide");
        guideBook.setColor(Color.CYAN);
        guideBook.setCategoryList(categories);
        guideBook.setRegistryName(rl("guidebook"));
        guideBook.setSpawnWithBook(ConfigHandler.misc.startWithGuide);

        return guideBook;
    }

    private static ResourceLocation rl(String res) {
        return new ResourceLocation(ModularRouters.MODID, res);
    }

    private static void buildEnhancementPages(Map<ResourceLocation, EntryAbstract> entries) {
        List<IPage> pages;

        pages = new ArrayList<>(PageHelper.pagesForLongText(translate("guidebook.para.fastPickupEnhancement"), MAX_PAGE_LENGTH));
        addEnhancementRecipePage(pages, new FastPickupEnhancementRecipe(ModuleType.VACUUM));
        entries.put(rl("moduleFastPickup"), new EntryItemStack(pages, translate("guidebook.words.fastPickup"), new ItemStack(Items.FISHING_ROD)));

        pages = new ArrayList<>(PageHelper.pagesForLongText(translate("guidebook.para.pickupDelayEnhancement"), MAX_PAGE_LENGTH));
        addEnhancementRecipePage(pages, new PickupDelayEnhancementRecipe(ModuleType.DROPPER));
        entries.put(rl("modulePickupDelay"), new EntryItemStack(pages, translate("guidebook.words.pickupDelay"), new ItemStack(Items.SLIME_BALL)));

        pages = new ArrayList<>(PageHelper.pagesForLongText(translate("guidebook.para.regulatorEnhancement"), MAX_PAGE_LENGTH));
        addEnhancementRecipePage(pages, new RegulatorEnhancementRecipe(ModuleType.PULLER));
        entries.put(rl("moduleRegulator"), new EntryItemStack(pages, translate("guidebook.words.regulator"), new ItemStack(Items.COMPARATOR)));

        pages = new ArrayList<>(PageHelper.pagesForLongText(translate("guidebook.para.redstoneEnhancement"), MAX_PAGE_LENGTH));
        addEnhancementRecipePage(pages, new RedstoneEnhancementRecipe(ModuleType.SENDER1));
        entries.put(rl("moduleRedstone"), new EntryItemStack(pages, translate("guidebook.words.redstone"), new ItemStack(Items.REDSTONE)));

        pages = new ArrayList<>(PageHelper.pagesForLongText(translate("guidebook.para.xpVacuumEnhancement"), MAX_PAGE_LENGTH));
        addEnhancementRecipePage(pages, new XPVacuumEnhancementRecipe(ModuleType.VACUUM));
        entries.put(rl("moduleXPVacuum"), new EntryItemStack(pages, translate("guidebook.words.xpVacuum"), new ItemStack(Items.EXPERIENCE_BOTTLE)));
    }

    private static void addEnhancementRecipePage(List<IPage> pages, ShapedOreRecipe recipe) {
        pages.add(new PageIRecipe(recipe, new ShapedOreRecipeRenderer(recipe)));
    }

    private static void buildFilterPages(Map<ResourceLocation, EntryAbstract> entries) {
        List<FilterType> types = Lists.newArrayList(FilterType.values()).stream()
                .map(ItemSmartFilter::makeItemStack)
                .sorted(Comparator.comparing(s -> translate(s.getUnlocalizedName())))
                .map(FilterType::getType)
                .collect(Collectors.toList());
        for (FilterType type : types) {
            ItemStack module = ItemSmartFilter.makeItemStack(type);
            String unlocalizedName = module.getItem().getUnlocalizedName(module);
            List<IPage> pages1 = Lists.newArrayList();
            pages1.add(new PageText(translate("itemText.usage." + unlocalizedName)));
            pages1.add(new PageIRecipe(ItemSmartFilter.getFilter(type).getRecipe()));
            String localizedName = translate(unlocalizedName + ".name");
            entries.put(rl(unlocalizedName), new EntryItemStack(pages1, localizedName, module));
        }
    }

    private static void buildModulePages(Map<ResourceLocation, EntryAbstract> entries) {
        // get a items of modules, sorted by their localized names
        List<ModuleType> types = Lists.newArrayList(ModuleType.values()).stream()
                .map(ModuleHelper::makeItemStack)
                .sorted(Comparator.comparing(s -> translate(s.getUnlocalizedName())))
                .map(ModuleType::getType)
                .collect(Collectors.toList());

        for (ModuleType type : types) {
            ItemStack module = ModuleHelper.makeItemStack(type);
            String unlocalizedName = module.getItem().getUnlocalizedName(module);
            List<IPage> pages1 = Lists.newArrayList();
            pages1.add(new PageText(translate("itemText.usage." + unlocalizedName, ItemModule.getModule(type).getExtraUsageParams())));
            pages1.add(new PageIRecipe(ItemModule.getModule(type).getRecipe()));
            String localizedName = translate(unlocalizedName + ".name");
            entries.put(rl(unlocalizedName), new EntryItemStack(pages1, localizedName, module));
        }
    }

    private static void buildUpgradePages(Map<ResourceLocation, EntryAbstract> entries) {
        // get a items of upgrades, sorted by their localized names
        List<UpgradeType> types = Lists.newArrayList(UpgradeType.values()).stream()
                .map(ItemUpgrade::makeItemStack)
                .sorted(Comparator.comparing(s -> translate(s.getUnlocalizedName())))
                .map(UpgradeType::getType)
                .collect(Collectors.toList());

        for (UpgradeType type : types) {
            ItemStack upgrade = ItemUpgrade.makeItemStack(type);
            String unlocalizedName = upgrade.getItem().getUnlocalizedName(upgrade);
            List<IPage> pages1 = Lists.newArrayList();
            pages1.add(new PageText(translate("itemText.usage." + unlocalizedName, ItemUpgrade.getUpgrade(type).getExtraUsageParams())));
            pages1.add(new PageIRecipe(ItemUpgrade.getUpgrade(type).getRecipe()));
            String localizedName = translate(unlocalizedName + ".name");
            entries.put(rl(unlocalizedName), new EntryItemStack(pages1, localizedName, upgrade));
        }
    }

    @Override
    @SideOnly(Side.CLIENT)
    public void handleModel(ItemStack bookStack) {
        GuideAPI.setModel(guideBook);
    }

    @Override
    public void handlePost(ItemStack bookStack) {
        GameRegistry.addShapelessRecipe(bookStack, ModItems.blankModule, Items.BOOK);
    }
}
