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
import android.view.ViewGroup;
import android.view.ContextMenu.ContextMenuInfo;
import android.widget.AdapterView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.SimpleCursorAdapter;
import android.widget.TextView;
import android.widget.SearchView;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;


public class NotesList extends ListActivity {

    private static final String TAG = "NotesList";

    private static final String[] PROJECTION = new String[] {
            NotePad.Notes._ID,
            NotePad.Notes.COLUMN_NAME_TITLE,
            NotePad.Notes.COLUMN_NAME_MODIFICATION_DATE,
            NotePad.Notes.COLUMN_NAME_CATEGORY
    };

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

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // The user does not need to hold down the key to use menu shortcuts.
        setDefaultKeyMode(DEFAULT_KEYS_SHORTCUT);

        Intent intent = getIntent();
        if (intent.getData() == null) {
            intent.setData(NotePad.Notes.CONTENT_URI);
        }

        getListView().setOnCreateContextMenuListener(this);
        setupAdapter();
    }

    private void setupAdapter() {
        Cursor cursor = getFilteredCursor();

        String[] dataColumns = {
                NotePad.Notes.COLUMN_NAME_TITLE,
                NotePad.Notes.COLUMN_NAME_MODIFICATION_DATE,
                NotePad.Notes.COLUMN_NAME_CATEGORY
        };

        int[] viewIDs = {
                android.R.id.text1,
                R.id.timestamp_text,
                R.id.category_text
        };

        mAdapter = new SimpleCursorAdapter(
                this,
                R.layout.noteslist_item,
                cursor,
                dataColumns,
                viewIDs
        ) {
            @Override
            public View getView(int position, View convertView, ViewGroup parent) {
                View view = super.getView(position, convertView, parent);

                // 获取当前项的数据
                Cursor cursor = (Cursor) getItem(position);
                if (cursor != null) {
                    String category = cursor.getString(COLUMN_INDEX_CATEGORY);
                    if (category == null) {
                        category = "未分类";
                    }

                    String title = cursor.getString(COLUMN_INDEX_TITLE);
                    long modified = cursor.getLong(COLUMN_INDEX_MODIFIED);

                    // 获取分类颜色和图标
                    int color = getCategoryColor(category);
                    String icon = getCategoryIcon(category);

                    // 设置分类标签
                    TextView categoryView = view.findViewById(R.id.category_text);
                    if (categoryView != null) {
                        GradientDrawable drawable = new GradientDrawable();
                        drawable.setColor(color);
                        drawable.setCornerRadius(12f);
                        categoryView.setBackground(drawable);
                        categoryView.setText(category);
                        categoryView.setTextColor(Color.WHITE);
                    }

                    // 设置图标
                    TextView iconView = view.findViewById(R.id.icon_view);
                    if (iconView != null) {
                        iconView.setText(icon);
                        iconView.setTextColor(color);
                    }

                    // 设置时间
                    TextView timeView = view.findViewById(R.id.timestamp_text);
                    if (timeView != null) {
                        timeView.setText(formatTimestamp(modified));
                    }

                    // 设置标题
                    TextView titleView = view.findViewById(android.R.id.text1);
                    if (titleView != null && title != null) {
                        titleView.setText(title);
                    }
                }
                return view;
            }

            @Override
            public void setViewText(TextView v, String text) {
                // 这里不设置文本，在getView中统一设置
                if (v.getId() == R.id.timestamp_text) {
                    // 时间戳在getView中设置
                    return;
                } else if (v.getId() == R.id.category_text) {
                    // 分类在getView中设置
                    return;
                } else {
                    super.setViewText(v, text);
                }
            }
        };

        setListAdapter(mAdapter);
    }

    // 获取分类颜色的辅助方法
    private int getCategoryColor(String category) {
        if (category == null) {
            return 0xFF607D8B;
        }
        switch (category) {
            case "工作": return 0xFFFF9800;
            case "个人": return 0xFF2196F3;
            case "想法": return 0xFF9C27B0;
            case "学习": return 0xFF4CAF50;
            case "待办事项": return 0xFFF44336;
            default: return 0xFF607D8B;
        }
    }

    // 获取分类图标的辅助方法
    private String getCategoryIcon(String category) {
        if (category == null) {
            return "📝";
        }
        switch (category) {
            case "工作": return "💼";
            case "个人": return "👤";
            case "想法": return "💡";
            case "学习": return "📚";
            case "待办事项": return "✓";
            default: return "📝";
        }
    }

    private Cursor getFilteredCursor() {
        String selection = null;
        String[] selectionArgs = null;

        if (!TextUtils.isEmpty(mCurrentSearchQuery)) {
            selection = "(" + NotePad.Notes.COLUMN_NAME_TITLE + " LIKE ? OR " +
                    NotePad.Notes.COLUMN_NAME_NOTE + " LIKE ?)";
            String searchArg = "%" + mCurrentSearchQuery + "%";
            selectionArgs = new String[]{searchArg, searchArg};
        }

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

        return managedQuery(
                getIntent().getData(),
                PROJECTION,
                selection,
                selectionArgs,
                NotePad.Notes.DEFAULT_SORT_ORDER
        );
    }

    private String formatTimestamp(long timestamp) {
        Date date = new Date(timestamp);
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault());
        return sdf.format(date);
    }

    private void refreshList() {
        Cursor newCursor = getFilteredCursor();
        mAdapter.changeCursor(newCursor);
        mAdapter.notifyDataSetChanged();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.list_options_menu, menu);

        MenuItem searchItem = menu.add(0, MENU_ITEM_SEARCH, 0, "搜索");
        searchItem.setIcon(android.R.drawable.ic_menu_search);
        searchItem.setShowAsAction(MenuItem.SHOW_AS_ACTION_IF_ROOM | MenuItem.SHOW_AS_ACTION_COLLAPSE_ACTION_VIEW);

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

        MenuItem categoryItem = menu.add(0, MENU_ITEM_CATEGORY, 1, "分类筛选");
        categoryItem.setIcon(android.R.drawable.ic_menu_sort_by_size);
        categoryItem.setShowAsAction(MenuItem.SHOW_AS_ACTION_NEVER);

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
            startActivity(new Intent(Intent.ACTION_INSERT, getIntent().getData()));
            return true;
        } else if (id == R.id.menu_paste) {
            startActivity(new Intent(Intent.ACTION_PASTE, getIntent().getData()));
            return true;
        } else if (id == MENU_ITEM_SEARCH) {
            return true;
        } else if (id == MENU_ITEM_CATEGORY) {
            showCategoryDialog();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void showCategoryDialog() {
        final String[] categories = {"所有", "工作", "个人", "想法", "学习", "待办事项", "未分类"};

        android.app.AlertDialog.Builder builder = new android.app.AlertDialog.Builder(this);
        builder.setTitle("选择分类");
        builder.setItems(categories, (dialog, which) -> {
            mCurrentCategory = categories[which];
            refreshList();
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

        ClipboardManager clipboard = (ClipboardManager)
                getSystemService(Context.CLIPBOARD_SERVICE);

        MenuItem mPasteItem = menu.findItem(R.id.menu_paste);

        if (clipboard.hasPrimaryClip()) {
            mPasteItem.setEnabled(true);
        } else {
            mPasteItem.setEnabled(false);
        }

        final boolean haveItems = getListAdapter().getCount() > 0;

        if (haveItems) {
            Uri uri = ContentUris.withAppendedId(getIntent().getData(), getSelectedItemId());

            Intent[] specifics = new Intent[1];
            specifics[0] = new Intent(Intent.ACTION_EDIT, uri);

            MenuItem[] items = new MenuItem[1];

            Intent intent = new Intent(null, uri);
            intent.addCategory(Intent.CATEGORY_ALTERNATIVE);

            menu.addIntentOptions(
                    Menu.CATEGORY_ALTERNATIVE,
                    Menu.NONE,
                    Menu.NONE,
                    null,
                    specifics,
                    intent,
                    Menu.NONE,
                    items
            );
            if (items[0] != null) {
                items[0].setShortcut('1', 'e');
            }
        } else {
            menu.removeGroup(Menu.CATEGORY_ALTERNATIVE);
        }

        return true;
    }

    @Override
    public void onCreateContextMenu(ContextMenu menu, View view, ContextMenuInfo menuInfo) {
        AdapterView.AdapterContextMenuInfo info;

        try {
            info = (AdapterView.AdapterContextMenuInfo) menuInfo;
        } catch (ClassCastException e) {
            Log.e(TAG, "bad menuInfo", e);
            return;
        }

        Cursor cursor = (Cursor) getListAdapter().getItem(info.position);

        if (cursor == null) {
            return;
        }

        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.list_context_menu, menu);

        menu.setHeaderTitle(cursor.getString(COLUMN_INDEX_TITLE));

        Intent intent = new Intent(null, Uri.withAppendedPath(getIntent().getData(),
                Integer.toString((int) info.id) ));
        intent.addCategory(Intent.CATEGORY_ALTERNATIVE);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        menu.addIntentOptions(Menu.CATEGORY_ALTERNATIVE, 0, 0,
                new ComponentName(this, NotesList.class), null, intent, 0, null);
    }

    @Override
    public boolean onContextItemSelected(MenuItem item) {
        AdapterView.AdapterContextMenuInfo info;

        try {
            info = (AdapterView.AdapterContextMenuInfo) item.getMenuInfo();
        } catch (ClassCastException e) {
            Log.e(TAG, "bad menuInfo", e);
            return false;
        }

        Uri noteUri = ContentUris.withAppendedId(getIntent().getData(), info.id);

        int id = item.getItemId();
        if (id == R.id.context_open) {
            startActivity(new Intent(Intent.ACTION_EDIT, noteUri));
            return true;
        } else if (id == R.id.context_copy) {
            ClipboardManager clipboard = (ClipboardManager)
                    getSystemService(Context.CLIPBOARD_SERVICE);

            clipboard.setPrimaryClip(ClipData.newUri(
                    getContentResolver(),
                    "Note",
                    noteUri));
            return true;
        } else if (id == R.id.context_delete) {
            getContentResolver().delete(
                    noteUri,
                    null,
                    null
            );
            refreshList();
            return true;
        }
        return super.onContextItemSelected(item);
    }

    @Override
    protected void onListItemClick(ListView l, View v, int position, long id) {
        Uri uri = ContentUris.withAppendedId(getIntent().getData(), id);

        String action = getIntent().getAction();

        if (Intent.ACTION_PICK.equals(action) || Intent.ACTION_GET_CONTENT.equals(action)) {
            setResult(RESULT_OK, new Intent().setData(uri));
        } else {
            startActivity(new Intent(Intent.ACTION_EDIT, uri));
        }
    }
}