# AndroidUI
# NotePad-Android 笔记应用

## 一、初始应用功能

### 1. 新建笔记和编辑笔记
(1) 在主界面点击添加按钮
(2) 进入笔记编辑界面后，可进行笔记内容编辑
<p><img width="381" height="201" alt="4b9b1f94f30e80ec7fffa0209c3aa821" src="https://github.com/user-attachments/assets/b6022b0b-6605-407c-be58-c5778b198547" />
</p>

### 2. 编辑标题
(1) 在笔记编辑界面中点击即可输入
<p><img width="384" height="266" alt="e2f87bc25b922469879f139d6c025911" src="https://github.com/user-attachments/assets/c571acbc-423a-4cd5-88a9-af9a47421da9" />
</p>

---

## 二、拓展基本功能

### （一）笔记条目增加时间戳显示

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
<p><img width="384" height="127" alt="image" src="https://github.com/user-attachments/assets/698c979d-0696-4052-8dc4-5952f2ef97b9" />
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
为笔记添加分类功能，支持按分类筛选和显示并且编辑页面也实现选择分类
<p><img width="379" height="79" alt="e47f5b9861b93529c0bc3553be42e72a" src="https://github.com/user-attachments/assets/ade7c2ef-cddc-47e9-a5d3-f1fa3fefb286" />
</p>

<p><img width="367" height="294" alt="6e8189a3-2861-426a-a340-726520e11522" src="https://github.com/user-attachments/assets/a01aa7c4-1c2f-4946-8a05-e6699f9de79f" />
</p>

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
    android:padding="8dp"
    android:background="#f5f5f5"
    android:gravity="center_vertical">

    <TextView
        android:layout_width="wrap_content"
        android:layout_height="wrap_content"
        android:text="分类:"
        android:textSize="16sp"
        android:textStyle="bold"
        android:paddingRight="8dp" />

    <Spinner
        android:id="@+id/category_spinner"
        android:layout_width="0dp"
        android:layout_height="wrap_content"
        android:layout_weight="1" />

</LinearLayout>
```

(3) 实现分类筛选
```java
private void showCategoryDialog() {
    final String[] categories = {"所有", "工作", "个人", "想法", "学习", "未分类"};
    // 显示分类选择对话框
}
```

#### 3. 实现效果
- 在编辑笔记时可选择分类
- 支持按分类筛选笔记
- 分类标签在列表中彩色显示

---

## 三、UI美化优化

### 1. 现代化界面设计
- 使用卡片式布局，圆角设计
- Material Design配色方案
- 优化的间距和字体大小

### 2. 动态分类颜色
```java
private void setCategoryColor(TextView categoryView, String category) {
    int color;
    switch (category) {
        case "工作":
            color = 0xFFFF9800; // 橙色
            break;
        case "个人":
            color = 0xFF2196F3; // 蓝色
            break;
        case "想法":
            color = 0xFF9C27B0; // 紫色
            break;
        case "学习":
            color = 0xFF4CAF50; // 绿色
            break;
        default:
            color = 0xFF607D8B; // 灰色
            break;
    }
    
    // 创建圆角背景
    GradientDrawable drawable = new GradientDrawable();
    drawable.setColor(color);
    drawable.setCornerRadius(12f);
    categoryView.setBackground(drawable);
}
```

### 3. 编辑器美化
- 优化编辑器背景和线条颜色并做旧背景
- 改进字体大小和行间距
- 更好的视觉层次
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


