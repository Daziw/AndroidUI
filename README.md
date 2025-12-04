# AndroidUI
# NotePad-Android 笔记应用

## 一、初始应用功能

### 1. 新建笔记和编辑笔记
(1) 在主界面点击添加按钮
(2) 进入笔记编辑界面后，可进行笔记内容编辑
<p><img width="385" height="135" alt="d23fef98d62666325586b3485f2d8b2b" src="https://github.com/user-attachments/assets/20f7e9e4-2f3b-4821-809a-cf49b627e14e" />
</p>

### 2. 编辑标题
(1) 在笔记编辑界面中点击即可输入
<p><img width="384" height="266" alt="e2f87bc25b922469879f139d6c025911" src="https://github.com/user-attachments/assets/c571acbc-423a-4cd5-88a9-af9a47421da9" />
</p>

---

## 二、拓展基本功能

### （一）笔记条目增加时间戳显示
<p><img width="184" height="66" alt="image" src="https://github.com/user-attachments/assets/a408484f-cc1c-4814-a199-1824937ffb24" />
</p>

#### 1. 功能要求
每个新建笔记都会保存新建时间并显示；在修改笔记后更新为修改时间

#### 2. 实现思路和技术实现
(1) 修改笔记列表布局，添加时间戳显示区域
```xml
<LinearLayout xmlns:android="http://schemas.android.com/apk/res/android"
    android:layout_width="match_parent"
    android:layout_height="wrap_content"
    android:orientation="vertical"
    android:padding="8dp">

    <!-- 标题和分类在同一行 -->
    <LinearLayout
        android:layout_width="match_parent"
        android:layout_height="wrap_content"
        android:orientation="horizontal">

        <!-- 标题 -->
        <TextView
            android:id="@android:id/text1"
            android:layout_width="0dp"
            android:layout_height="wrap_content"
            android:layout_weight="1"
            android:textAppearance="?android:attr/textAppearanceLarge"
            android:paddingLeft="5dp"
            android:singleLine="true" />

        <!-- 分类 -->
        <TextView
            android:id="@+id/category_text"
            android:layout_width="wrap_content"
            android:layout_height="wrap_content"
            android:background="#FF4081"
            android:textColor="#FFFFFF"
            android:textSize="10sp"
            android:padding="4dp"
            android:maxLines="1" />

    </LinearLayout>

    <!-- 时间戳 -->
    <TextView
        android:id="@+id/timestamp_text"
        android:layout_width="match_parent"
        android:layout_height="wrap_content"
        android:textAppearance="?android:attr/textAppearanceSmall"
        android:textColor="#666666"
        android:paddingLeft="5dp"
        android:paddingTop="2dp" />

</LinearLayout>
```

(2) 在PROJECTION中添加时间戳字段
```java
private static final String[] PROJECTION = new String[] {
        NotePad.Notes._ID, // 0
        NotePad.Notes.COLUMN_NAME_TITLE, // 1
        NotePad.Notes.COLUMN_NAME_MODIFICATION_DATE, // 2 - 时间戳字段
        NotePad.Notes.COLUMN_NAME_CATEGORY // 3 - 分类字段
};
```

(3) 更新数据列映射
```java
String[] dataColumns = { 
    NotePad.Notes.COLUMN_NAME_TITLE,
    NotePad.Notes.COLUMN_NAME_MODIFICATION_DATE,
    NotePad.Notes.COLUMN_NAME_CATEGORY
};

int[] viewIDs = { 
    android.R.id.text1,      // 标题
    R.id.timestamp_text,     // 时间戳
    R.id.category_text       // 分类
};
```

(4) 格式化时间戳显示
```java
private String formatTimestamp(long timestamp) {
    Date date = new Date(timestamp);
    SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault());
    return sdf.format(date);
}
```

#### 3. 实现效果
- 创建笔记时显示创建时间
- 修改笔记后显示的时间更新为最新修改的时间

### （二）笔记查询功能（根据标题或内容查询）

#### 1. 功能要求
支持按标题和内容搜索笔记，实时显示搜索结果
<p><img width="383" height="127" alt="image" src="https://github.com/user-attachments/assets/a6a588eb-89bf-4c86-92f7-315dc7a0ed69" />
</p>

#### 2. 实现思路和技术实现
(1) 在菜单中添加搜索功能
```java
// 添加搜索菜单项
MenuItem searchItem = menu.add(0, MENU_ITEM_SEARCH, 0, "搜索");
searchItem.setIcon(android.R.drawable.ic_menu_search);
searchItem.setShowAsAction(MenuItem.SHOW_AS_ACTION_IF_ROOM | MenuItem.SHOW_AS_ACTION_COLLAPSE_ACTION_VIEW);
```

(2) 实现搜索逻辑
```java
private Cursor getFilteredCursor() {
    String selection = null;
    String[] selectionArgs = null;

    // 构建搜索条件
    if (!TextUtils.isEmpty(mCurrentSearchQuery)) {
        selection = "(" + NotePad.Notes.COLUMN_NAME_TITLE + " LIKE ? OR " + 
                    NotePad.Notes.COLUMN_NAME_NOTE + " LIKE ?)";
        String searchArg = "%" + mCurrentSearchQuery + "%";
        selectionArgs = new String[]{searchArg, searchArg};
    }
    // ... 其他筛选条件
}
```

(3) 实时搜索功能
```java
searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
    @Override
    public boolean onQueryTextChange(String newText) {
        mCurrentSearchQuery = newText;
        refreshList();
        return true;
    }
});
```

#### 3. 实现效果
- 输入搜索内容时实时显示匹配的笔记
- 支持标题和内容的模糊搜索
- 清空搜索内容后显示所有笔记

### （三）笔记分类功能

#### 1. 功能要求
-为笔记添加分类功能
<p><img width="367" height="294" alt="6e8189a3-2861-426a-a340-726520e11522" src="https://github.com/user-attachments/assets/a01aa7c4-1c2f-4946-8a05-e6699f9de79f" />
</p>
-支持按分类筛选和显示并且编辑页面实现选择分类
<p><img width="385" height="187" alt="211d5d916a4cdad07fed49d51e8f4fe7" src="https://github.com/user-attachments/assets/bfa271ce-e56b-4723-a40a-eed4a0f5039f" /></p>
<p><img width="359" height="399" alt="image" src="https://github.com/user-attachments/assets/697add31-32a4-4c08-89c1-21f3991f3864" /></p>
<p><img width="388" height="136" alt="image" src="https://github.com/user-attachments/assets/647cf7f1-cb29-4b8f-9f0a-05461785dfc6" /></p>


#### 2. 实现思路和技术实现
(1) 在数据库中添加分类字段
```java
// 在NotePad.java中添加分类常量
public static final String COLUMN_NAME_CATEGORY = "category";

// 在数据库创建时添加分类字段
db.execSQL("CREATE TABLE " + NotePad.Notes.TABLE_NAME + " ("
        + NotePad.Notes._ID + " INTEGER PRIMARY KEY,"
        + NotePad.Notes.COLUMN_NAME_TITLE + " TEXT,"
        + NotePad.Notes.COLUMN_NAME_NOTE + " TEXT,"
        + NotePad.Notes.COLUMN_NAME_CREATE_DATE + " INTEGER,"
        + NotePad.Notes.COLUMN_NAME_MODIFICATION_DATE + " INTEGER,"
        + NotePad.Notes.COLUMN_NAME_CATEGORY + " TEXT DEFAULT '未分类'"
        + ");");
```

(2) 在编辑界面添加分类选择
```xml
<!-- 分类选择行 -->
<LinearLayout
    android:layout_width="match_parent"
    android:layout_height="wrap_content"
    android:orientation="horizontal"
    android:padding="12dp"
    android:background="#FFE8D4A8"
    android:gravity="center_vertical"
    android:layout_margin="8dp">

    <TextView
        android:layout_width="wrap_content"
        android:layout_height="wrap_content"
        android:text="📁 分类:"
        android:textSize="16sp"
        android:textStyle="bold"
        android:textColor="#5D4037"
        android:paddingRight="8dp" />

    <Spinner
        android:id="@+id/category_spinner"
        android:layout_width="0dp"
        android:layout_height="wrap_content"
        android:layout_weight="1"
        android:background="#FFF5E6C2"
        android:padding="8dp" />

</LinearLayout>
```

(3) 实现分类筛选功能
```java
// 在菜单中添加分类筛选选项
@Override
public boolean onCreateOptionsMenu(Menu menu) {
    // 添加分类筛选菜单项
    MenuItem filterItem = menu.add(0, MENU_ITEM_FILTER, 1, "📂 按分类筛选");
    filterItem.setShowAsAction(MenuItem.SHOW_AS_ACTION_IF_ROOM);
    
    // 添加子菜单项
    SubMenu filterSubMenu = menu.addSubMenu("📂 按分类筛选");
    filterSubMenu.add(0, FILTER_ALL, 0, "📋 所有笔记");
    filterSubMenu.add(0, FILTER_WORK, 1, "💼 工作");
    filterSubMenu.add(0, FILTER_PERSONAL, 2, "🏠 个人");
    filterSubMenu.add(0, FILTER_IDEA, 3, "💡 想法");
    filterSubMenu.add(0, FILTER_STUDY, 4, "📚 学习");
    filterSubMenu.add(0, FILTER_UNCATEGORIZED, 5, "❓ 未分类");
    
    return super.onCreateOptionsMenu(menu);
}

// 处理分类筛选选择
@Override
public boolean onOptionsItemSelected(MenuItem item) {
    switch (item.getItemId()) {
        case FILTER_ALL:
            mCurrentCategory = null;
            break;
        case FILTER_WORK:
            mCurrentCategory = "工作";
            break;
        case FILTER_PERSONAL:
            mCurrentCategory = "个人";
            break;
        case FILTER_IDEA:
            mCurrentCategory = "想法";
            break;
        case FILTER_STUDY:
            mCurrentCategory = "学习";
            break;
        case FILTER_UNCATEGORIZED:
            mCurrentCategory = "未分类";
            break;
    }
    refreshList(); // 刷新列表显示筛选结果
    return true;
}
```

(4) 更新查询逻辑以支持分类筛选
```java
private Cursor getFilteredCursor() {
    String selection = null;
    String[] selectionArgs = null;
    List<String> selectionList = new ArrayList<>();
    List<String> argsList = new ArrayList<>();

    // 搜索条件
    if (!TextUtils.isEmpty(mCurrentSearchQuery)) {
        selectionList.add("(" + NotePad.Notes.COLUMN_NAME_TITLE + " LIKE ? OR " + 
                         NotePad.Notes.COLUMN_NAME_NOTE + " LIKE ?)");
        String searchArg = "%" + mCurrentSearchQuery + "%";
        argsList.add(searchArg);
        argsList.add(searchArg);
    }

    // 分类筛选条件
    if (mCurrentCategory != null) {
        if ("未分类".equals(mCurrentCategory)) {
            selectionList.add(NotePad.Notes.COLUMN_NAME_CATEGORY + " IS NULL OR " + 
                            NotePad.Notes.COLUMN_NAME_CATEGORY + " = ?");
        } else {
            selectionList.add(NotePad.Notes.COLUMN_NAME_CATEGORY + " = ?");
        }
        argsList.add(mCurrentCategory);
    }

    // 组合所有条件
    if (!selectionList.isEmpty()) {
        selection = TextUtils.join(" AND ", selectionList);
        selectionArgs = argsList.toArray(new String[0]);
    }

    return getContentResolver().query(
        getIntent().getData(),
        PROJECTION,
        selection,
        selectionArgs,
        NotePad.Notes.DEFAULT_SORT_ORDER
    );
}
```

#### 3. 实现效果
- 在编辑笔记时可选择分类，使用复古风格的图标和配色
- 支持按分类筛选笔记，菜单项带有相应图标
- 分类标签在列表中彩色显示

---

## 三、UI美化优化

### 1. 现代化界面设计
- 使用卡片式布局，圆角设计
- Material Design配色方案
- 优化的间距和字体大小

### 2. 动态分类颜色和图标系统
- 左侧图标跟随分类种类变化
- 不同种类卡片颜色不同
<p><img width="385" height="427" alt="image" src="https://github.com/user-attachments/assets/83c30d79-6c42-4fff-ae2d-9c523652e68a" />
</p>

```java
private void setCategoryColorAndIcon(TextView categoryView, String category) {
    int color;
    String icon;
    
    switch (category) {
        case "工作":
            color = 0xFFFF9800; // 橙色
            icon = "💼";
            break;
        case "个人":
            color = 0xFF2196F3; // 蓝色
            icon = "🏠";
            break;
        case "想法":
            color = 0xFF9C27B0; // 紫色
            icon = "💡";
            break;
        case "学习":
            color = 0xFF4CAF50; // 绿色
            icon = "📚";
            break;
        default:
            color = 0xFF607D8B; // 灰色
            icon = "❓";
            break;
    }
    
    // 创建圆角背景
    GradientDrawable drawable = new GradientDrawable();
    drawable.setColor(color);
    drawable.setCornerRadius(12f);
    drawable.setStroke(1, color & 0x80FFFFFF); // 添加半透明边框
    
    // 设置文本和样式
    categoryView.setText(icon + " " + category);
    categoryView.setBackground(drawable);
    categoryView.setTextColor(Color.WHITE);
    categoryView.setPadding(8, 4, 8, 4);
    categoryView.setTypeface(categoryView.getTypeface(), Typeface.BOLD);
}
```

### 3. 编辑器美化和复古主题
(1) 编辑器界面美化
```xml
<!-- 复古风格的背景 -->
<ScrollView
    android:layout_width="match_parent"
    android:layout_height="match_parent"
    android:background="@drawable/vintage_background">

    <!-- 标题输入区域 -->
    <LinearLayout
        android:layout_width="match_parent"
        android:layout_height="wrap_content"
        android:orientation="vertical"
        android:padding="16dp"
        android:background="#FFFAF3E0">

        <!-- 标题输入框 -->
        <EditText
            android:id="@+id/title"
            android:layout_width="match_parent"
            android:layout_height="wrap_content"
            android:hint="✏️ 笔记标题"
            android:textSize="20sp"
            android:textStyle="bold"
            android:background="@drawable/vintage_edittext"
            android:padding="12dp"
            android:textColor="#5D4037" />

        <!-- 分类选择行（已在前文展示） -->
        
        <!-- 内容输入区域 -->
        <EditText
            android:id="@+id/note"
            android:layout_width="match_parent"
            android:layout_height="wrap_content"
            android:layout_weight="1"
            android:gravity="top"
            android:hint="📝 开始记录..."
            android:background="@drawable/vintage_textarea"
            android:padding="16dp"
            android:textSize="16sp"
            android:minHeight="300dp"
            android:textColor="#4E342E"
            android:lineSpacingExtra="4dp" />

    </LinearLayout>
</ScrollView>
```

(2) 复古背景和边框资源
```xml
<!-- res/drawable/vintage_background.xml -->
<shape xmlns:android="http://schemas.android.com/apk/res/android">
    <solid android:color="#FFF5E6C2" />
    <stroke android:width="1dp" android:color="#E0C9A6" />
</shape>

<!-- res/drawable/vintage_edittext.xml -->
<shape xmlns:android="http://schemas.android.com/apk/res/android">
    <solid android:color="#FFF9F4E9" />
    <corners android:radius="8dp" />
    <stroke android:width="2dp" android:color="#D7CCC8" />
    <padding android:left="8dp" android:top="8dp" android:right="8dp" android:bottom="8dp" />
</shape>

<!-- res/drawable/vintage_textarea.xml -->
<shape xmlns:android="http://schemas.android.com/apk/res/android">
    <solid android:color="#FFFCF8F0" />
    <corners android:radius="12dp" />
    <stroke android:width="3dp" android:color="#BCAAA4" />
    <padding android:left="12dp" android:top="12dp" android:right="12dp" android:bottom="12dp" />
</shape>
```

(3) 主界面列表项美化
```xml
<!-- 列表项卡片式布局 -->
<androidx.cardview.widget.CardView
    xmlns:android="http://schemas.android.com/apk/res/android"
    xmlns:app="http://schemas.android.com/apk/res-auto"
    android:layout_width="match_parent"
    android:layout_height="wrap_content"
    android:layout_margin="8dp"
    app:cardCornerRadius="12dp"
    app:cardElevation="4dp"
    app:cardUseCompatPadding="true">

    <LinearLayout
        android:layout_width="match_parent"
        android:layout_height="wrap_content"
        android:orientation="vertical"
        android:padding="16dp"
        android:background="#FFFAF3E0">

        <!-- 标题行 -->
        <LinearLayout
            android:layout_width="match_parent"
            android:layout_height="wrap_content"
            android:orientation="horizontal"
            android:gravity="center_vertical">

            <!-- 标题图标和文本 -->
            <TextView
                android:id="@android:id/text1"
                android:layout_width="0dp"
                android:layout_height="wrap_content"
                android:layout_weight="1"
                android:textSize="18sp"
                android:textStyle="bold"
                android:textColor="#5D4037"
                android:singleLine="true"
                android:drawablePadding="8dp"
                android:drawableStart="@drawable/ic_note_icon" />

            <!-- 分类标签 -->
            <TextView
                android:id="@+id/category_text"
                android:layout_width="wrap_content"
                android:layout_height="wrap_content"
                android:textSize="12sp"
                android:padding="6dp"
                android:layout_marginStart="8dp" />

        </LinearLayout>

        <!-- 时间戳行 -->
        <LinearLayout
            android:layout_width="match_parent"
            android:layout_height="wrap_content"
            android:orientation="horizontal"
            android:gravity="center_vertical"
            android:layout_marginTop="8dp">

            <ImageView
                android:layout_width="16dp"
                android:layout_height="16dp"
                android:src="@drawable/ic_time_icon"
                android:tint="#795548" />

            <TextView
                android:id="@+id/timestamp_text"
                android:layout_width="wrap_content"
                android:layout_height="wrap_content"
                android:textSize="14sp"
                android:textColor="#795548"
                android:layout_marginStart="4dp" />

        </LinearLayout>

    </LinearLayout>
</androidx.cardview.widget.CardView>
```

### 4. 应用图标和菜单图标更新
(1) 应用主题配置
```xml
<!-- res/values/themes.xml -->
<resources xmlns:tools="http://schemas.android.com/tools">
    <style name="Theme.NotePad" parent="Theme.MaterialComponents.Light.NoActionBar">
        <item name="colorPrimary">#5D4037</item>
        <item name="colorPrimaryVariant">#3E2723</item>
        <item name="colorOnPrimary">#FFFFFF</item>
        <item name="colorSecondary">#FF9800</item>
        <item name="colorSecondaryVariant">#F57C00</item>
        <item name="colorOnSecondary">#FFFFFF</item>
        <item name="android:statusBarColor">#3E2723</item>
        <item name="android:windowBackground">#FFF5E6C2</item>
    </style>
</resources>
```

**界面特点：**
- 📁 分类选择：复古文件夹图标配合棕色主题
- 📂 筛选功能：文件柜图标，清晰的分类筛选菜单
- ✏️ 编辑界面：羽毛笔图标，做旧纸张背景
- 📝 内容区域：复古边框，舒适的阅读体验
- 🕐 时间显示：复古时钟图标，统一的设计语言

