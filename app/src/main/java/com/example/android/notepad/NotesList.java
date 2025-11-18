/*
 * Copyright (C) 2007 The Android Open Source Project
 *
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
 */

package com.example.android.notepad;

import com.example.android.notepad.NotePad;

import android.app.ListActivity;
import android.content.ClipboardManager;
import android.content.ClipData;
import android.content.ComponentName;
import android.content.ContentUris;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.ContextMenu;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ContextMenu.ContextMenuInfo;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.SimpleCursorAdapter;
import android.widget.TextView;
import android.widget.SearchView;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;


/**
 * Displays a list of notes. Will display notes from the {@link Uri}
 * provided in the incoming Intent if there is one, otherwise it defaults to displaying the
 * contents of the {@link NotePadProvider}.
 *
 * NOTE: Notice that the provider operations in this Activity are taking place on the UI thread.
 * This is not a good practice. It is only done here to make the code more readable. A real
 * application should use the {@link android.content.AsyncQueryHandler} or
 * {@link android.os.AsyncTask} object to perform operations asynchronously on a separate thread.
 */
public class NotesList extends ListActivity {

    // For logging and debugging
    private static final String TAG = "NotesList";

    /**
     * The columns needed by the cursor adapter
     */
    private static final String[] PROJECTION = new String[] {
            NotePad.Notes._ID, // 0
            NotePad.Notes.COLUMN_NAME_TITLE, // 1
            NotePad.Notes.COLUMN_NAME_MODIFICATION_DATE, // 2 - 时间戳字段
            NotePad.Notes.COLUMN_NAME_CATEGORY // 3 - 分类字段
    };

    /** The index of the title column */
    private static final int COLUMN_INDEX_TITLE = 1;
    private static final int COLUMN_INDEX_MODIFIED = 2;
    private static final int COLUMN_INDEX_CATEGORY = 3;

    // 菜单项ID
    private static final int MENU_ITEM_SEARCH = 100;
    private static final int MENU_ITEM_CATEGORY = 101;

    // 搜索和分类相关变量
    private String mCurrentSearchQuery = "";
    private String mCurrentCategory = "所有";
    private SimpleCursorAdapter mAdapter;

    /**
     * onCreate is called when Android starts this Activity from scratch.
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // The user does not need to hold down the key to use menu shortcuts.
        setDefaultKeyMode(DEFAULT_KEYS_SHORTCUT);

        /* If no data is given in the Intent that started this Activity, then this Activity
         * was started when the intent filter matched a MAIN action. We should use the default
         * provider URI.
         */
        // Gets the intent that started this Activity.
        Intent intent = getIntent();

        // If there is no data associated with the Intent, sets the data to the default URI, which
        // accesses a list of notes.
        if (intent.getData() == null) {
            intent.setData(NotePad.Notes.CONTENT_URI);
        }

        /*
         * Sets the callback for context menu activation for the ListView. The listener is set
         * to be this Activity. The effect is that context menus are enabled for items in the
         * ListView, and the context menu is handled by a method in NotesList.
         */
        getListView().setOnCreateContextMenuListener(this);

        // 初始化适配器并设置列表
        setupAdapter();
    }

    /**
     * 设置列表适配器
     */
    private void setupAdapter() {
        // 执行查询
        Cursor cursor = getFilteredCursor();

        /*
         * The following two arrays create a "map" between columns in the cursor and view IDs
         * for items in the ListView. Each element in the dataColumns array represents
         * a column name; each element in the viewID array represents the ID of a View.
         * The SimpleCursorAdapter maps them in ascending order to determine where each column
         * value will appear in the ListView.
         */

        // The names of the cursor columns to display in the view
        String[] dataColumns = {
                NotePad.Notes.COLUMN_NAME_TITLE,
                NotePad.Notes.COLUMN_NAME_MODIFICATION_DATE,
                NotePad.Notes.COLUMN_NAME_CATEGORY
        };

        // The view IDs that will display the cursor columns
        int[] viewIDs = {
                android.R.id.text1,      // 标题
                R.id.timestamp_text,     // 时间戳
                R.id.category_text       // 分类
        };

        // Creates the backing adapter for the ListView.
        mAdapter = new SimpleCursorAdapter(
                this,                             // The Context for the ListView
                R.layout.noteslist_item,          // Points to the XML for a list item
                cursor,                           // The cursor to get items from
                dataColumns,
                viewIDs
        ) {
            @Override
            public void setViewText(TextView v, String text) {
                if (v.getId() == R.id.timestamp_text) {
                    // 格式化时间戳显示
                    if (!TextUtils.isEmpty(text)) {
                        try {
                            long timestamp = Long.parseLong(text);
                            String timeString = formatTimestamp(timestamp);
                            super.setViewText(v, timeString);
                        } catch (NumberFormatException e) {
                            super.setViewText(v, "未知时间");
                        }
                    } else {
                        super.setViewText(v, "未知时间");
                    }
                } else if (v.getId() == R.id.category_text) {
                    // 显示分类并设置颜色
                    if (!TextUtils.isEmpty(text)) {
                        super.setViewText(v, text);
                        setCategoryColor(v, text);
                    } else {
                        super.setViewText(v, "未分类");
                        setCategoryColor(v, "未分类");
                    }
                } else {
                    super.setViewText(v, text);
                }
            }
        };

        // Sets the ListView's adapter to be the cursor adapter that was just created.
        setListAdapter(mAdapter);
    }

    /**
     * 根据分类设置不同的颜色
     */
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
                color = 0xFF607D8B; // 灰色（未分类）
                break;
        }

        // 创建圆角背景
        GradientDrawable drawable = new GradientDrawable();
        drawable.setColor(color);
        drawable.setCornerRadius(12f); // 圆角半径

        categoryView.setBackground(drawable);
    }

    /**
     * 获取过滤后的游标（支持搜索和分类筛选）
     */
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

        // 构建分类条件
        if (!"所有".equals(mCurrentCategory)) {
            if (selection == null) {
                selection = NotePad.Notes.COLUMN_NAME_CATEGORY + " = ?";
                selectionArgs = new String[]{mCurrentCategory};
            } else {
                selection += " AND " + NotePad.Notes.COLUMN_NAME_CATEGORY + " = ?";
                String[] newArgs = new String[selectionArgs.length + 1];
                System.arraycopy(selectionArgs, 0, newArgs, 0, selectionArgs.length);
                newArgs[selectionArgs.length] = mCurrentCategory;
                selectionArgs = newArgs;
            }
        }

        /* Performs a managed query. The Activity handles closing and requerying the cursor
         * when needed.
         */
        return managedQuery(
                getIntent().getData(),            // Use the default content URI for the provider.
                PROJECTION,                       // Return the note ID, title, timestamp and category.
                selection,                        // Where clause
                selectionArgs,                    // Where args
                NotePad.Notes.DEFAULT_SORT_ORDER  // Use the default sort order.
        );
    }

    /**
     * 格式化时间戳
     */
    private String formatTimestamp(long timestamp) {
        Date date = new Date(timestamp);
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault());
        return sdf.format(date);
    }

    /**
     * 刷新列表数据
     */
    private void refreshList() {
        Cursor newCursor = getFilteredCursor();
        mAdapter.changeCursor(newCursor);
        mAdapter.notifyDataSetChanged();
    }

    /**
     * Called when the user clicks the device's Menu button the first time for
     * this Activity. Android passes in a Menu object that is populated with items.
     */
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate menu from XML resource
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.list_options_menu, menu);

        // 添加搜索菜单项
        MenuItem searchItem = menu.add(0, MENU_ITEM_SEARCH, 0, "搜索");
        searchItem.setIcon(android.R.drawable.ic_menu_search);
        searchItem.setShowAsAction(MenuItem.SHOW_AS_ACTION_IF_ROOM | MenuItem.SHOW_AS_ACTION_COLLAPSE_ACTION_VIEW);

        // 创建搜索视图
        SearchView searchView = new SearchView(this);
        searchView.setQueryHint("搜索笔记标题");
        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                return false;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                mCurrentSearchQuery = newText;
                refreshList();
                return true;
            }
        });
        searchItem.setActionView(searchView);

        // 添加分类筛选菜单
        MenuItem categoryItem = menu.add(0, MENU_ITEM_CATEGORY, 1, "分类筛选");
        categoryItem.setIcon(android.R.drawable.ic_menu_sort_by_size);
        categoryItem.setShowAsAction(MenuItem.SHOW_AS_ACTION_NEVER);

        // 生成任何可以在此列表上执行的附加操作
        Intent intent = new Intent(null, getIntent().getData());
        intent.addCategory(Intent.CATEGORY_ALTERNATIVE);
        menu.addIntentOptions(Menu.CATEGORY_ALTERNATIVE, 0, 0,
                new ComponentName(this, NotesList.class), null, intent, 0, null);

        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.menu_add) {
            // 启动新笔记Activity
            startActivity(new Intent(Intent.ACTION_INSERT, getIntent().getData()));
            return true;
        } else if (id == R.id.menu_paste) {
            // 启动粘贴Activity
            startActivity(new Intent(Intent.ACTION_PASTE, getIntent().getData()));
            return true;
        } else if (id == MENU_ITEM_SEARCH) {
            // 搜索功能已经在SearchView中处理
            return true;
        } else if (id == MENU_ITEM_CATEGORY) {
            // 显示分类选择对话框
            showCategoryDialog();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    /**
     * 显示分类选择对话框
     */
    private void showCategoryDialog() {
        final String[] categories = {"所有", "工作", "个人", "想法", "学习", "未分类"};

        android.app.AlertDialog.Builder builder = new android.app.AlertDialog.Builder(this);
        builder.setTitle("选择分类");
        builder.setItems(categories, (dialog, which) -> {
            mCurrentCategory = categories[which];
            refreshList();
            // 在标题栏显示当前分类
            if ("所有".equals(mCurrentCategory)) {
                setTitle("所有笔记");
            } else {
                setTitle("分类: " + mCurrentCategory);
            }
        });
        builder.show();
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        super.onPrepareOptionsMenu(menu);

        // The paste menu item is enabled if there is data on the clipboard.
        ClipboardManager clipboard = (ClipboardManager)
                getSystemService(Context.CLIPBOARD_SERVICE);

        MenuItem mPasteItem = menu.findItem(R.id.menu_paste);

        // If the clipboard contains an item, enables the Paste option on the menu.
        if (clipboard.hasPrimaryClip()) {
            mPasteItem.setEnabled(true);
        } else {
            // If the clipboard is empty, disables the menu's Paste option.
            mPasteItem.setEnabled(false);
        }

        // Gets the number of notes currently being displayed.
        final boolean haveItems = getListAdapter().getCount() > 0;

        // If there are any notes in the list (which implies that one of
        // them is selected), then we need to generate the actions that
        // can be performed on the current selection.
        if (haveItems) {

            // This is the selected item.
            Uri uri = ContentUris.withAppendedId(getIntent().getData(), getSelectedItemId());

            // Creates an array of Intents with one element. This will be used to send an Intent
            // based on the selected menu item.
            Intent[] specifics = new Intent[1];

            // Sets the Intent in the array to be an EDIT action on the URI of the selected note.
            specifics[0] = new Intent(Intent.ACTION_EDIT, uri);

            // Creates an array of menu items with one element. This will contain the EDIT option.
            MenuItem[] items = new MenuItem[1];

            // Creates an Intent with no specific action, using the URI of the selected note.
            Intent intent = new Intent(null, uri);

            /* Adds the category ALTERNATIVE to the Intent, with the note ID URI as its
             * data. This prepares the Intent as a place to group alternative options in the
             * menu.
             */
            intent.addCategory(Intent.CATEGORY_ALTERNATIVE);

            /*
             * Add alternatives to the menu
             */
            menu.addIntentOptions(
                    Menu.CATEGORY_ALTERNATIVE,  // Add the Intents as options in the alternatives group.
                    Menu.NONE,                  // A unique item ID is not required.
                    Menu.NONE,                  // The alternatives don't need to be in order.
                    null,                       // The caller's name is not excluded from the group.
                    specifics,                  // These specific options must appear first.
                    intent,                     // These Intent objects map to the options in specifics.
                    Menu.NONE,                  // No flags are required.
                    items                       // The menu items generated from the specifics-to-
                    // Intents mapping
            );
            // If the Edit menu item exists, adds shortcuts for it.
            if (items[0] != null) {
                // Sets the Edit menu item shortcut to numeric "1", letter "e"
                items[0].setShortcut('1', 'e');
            }
        } else {
            // If the list is empty, removes any existing alternative actions from the menu
            menu.removeGroup(Menu.CATEGORY_ALTERNATIVE);
        }

        // Displays the menu
        return true;
    }

    /**
     * This method is called when the user context-clicks a note in the list.
     */
    @Override
    public void onCreateContextMenu(ContextMenu menu, View view, ContextMenuInfo menuInfo) {

        // The data from the menu item.
        AdapterView.AdapterContextMenuInfo info;

        // Tries to get the position of the item in the ListView that was long-pressed.
        try {
            // Casts the incoming data object into the type for AdapterView objects.
            info = (AdapterView.AdapterContextMenuInfo) menuInfo;
        } catch (ClassCastException e) {
            // If the menu object can't be cast, logs an error.
            Log.e(TAG, "bad menuInfo", e);
            return;
        }

        /*
         * Gets the data associated with the item at the selected position.
         */
        Cursor cursor = (Cursor) getListAdapter().getItem(info.position);

        // If the cursor is empty, then for some reason the adapter can't get the data from the
        // provider, so returns null to the caller.
        if (cursor == null) {
            // For some reason the requested item isn't available, do nothing
            return;
        }

        // Inflate menu from XML resource
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.list_context_menu, menu);

        // Sets the menu header to be the title of the selected note.
        menu.setHeaderTitle(cursor.getString(COLUMN_INDEX_TITLE));

        // Append to the menu items for any other activities that can do stuff with it
        // as well.
        Intent intent = new Intent(null, Uri.withAppendedPath(getIntent().getData(),
                Integer.toString((int) info.id) ));
        intent.addCategory(Intent.CATEGORY_ALTERNATIVE);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        menu.addIntentOptions(Menu.CATEGORY_ALTERNATIVE, 0, 0,
                new ComponentName(this, NotesList.class), null, intent, 0, null);
    }

    /**
     * This method is called when the user selects an item from the context menu
     */
    @Override
    public boolean onContextItemSelected(MenuItem item) {
        // The data from the menu item.
        AdapterView.AdapterContextMenuInfo info;

        try {
            // Casts the data object in the item into the type for AdapterView objects.
            info = (AdapterView.AdapterContextMenuInfo) item.getMenuInfo();
        } catch (ClassCastException e) {
            // If the object can't be cast, logs an error
            Log.e(TAG, "bad menuInfo", e);
            // Triggers default processing of the menu item.
            return false;
        }

        // Appends the selected note's ID to the URI sent with the incoming Intent.
        Uri noteUri = ContentUris.withAppendedId(getIntent().getData(), info.id);

        /*
         * Gets the menu item's ID and compares it to known actions.
         */
        int id = item.getItemId();
        if (id == R.id.context_open) {
            // Launch activity to view/edit the currently selected item
            startActivity(new Intent(Intent.ACTION_EDIT, noteUri));
            return true;
        } else if (id == R.id.context_copy) {
            // Gets a handle to the clipboard service.
            ClipboardManager clipboard = (ClipboardManager)
                    getSystemService(Context.CLIPBOARD_SERVICE);

            // Copies the notes URI to the clipboard.
            clipboard.setPrimaryClip(ClipData.newUri(
                    getContentResolver(),
                    "Note",
                    noteUri));
            return true;
        } else if (id == R.id.context_delete) {
            // Deletes the note from the provider
            getContentResolver().delete(
                    noteUri,
                    null,
                    null
            );
            // 刷新列表
            refreshList();
            return true;
        }
        return super.onContextItemSelected(item);
    }

    /**
     * This method is called when the user clicks a note in the displayed list.
     */
    @Override
    protected void onListItemClick(ListView l, View v, int position, long id) {
        // Constructs a new URI from the incoming URI and the row ID
        Uri uri = ContentUris.withAppendedId(getIntent().getData(), id);

        // Gets the action from the incoming Intent
        String action = getIntent().getAction();

        // Handles requests for note data
        if (Intent.ACTION_PICK.equals(action) || Intent.ACTION_GET_CONTENT.equals(action)) {
            // Sets the result to return to the component that called this Activity.
            setResult(RESULT_OK, new Intent().setData(uri));
        } else {
            // Sends out an Intent to start an Activity that can handle ACTION_EDIT.
            startActivity(new Intent(Intent.ACTION_EDIT, uri));
        }
    }
}