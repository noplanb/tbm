package com.zazoapp.client.bench;

/**
 * http://stackoverflow.com/questions/26755878/how-can-i-fix-the-spinner-style-for-android-4-x-placed-on-top-of-the-toolbar
 * Created by skamenkovych@codeminders.com on 11/20/2015.
 */
//public class ContactsGroupAdapter extends BaseAdapter {
//    private List<YourObject> mItems = new ArrayList<>();
//
//    public void clear() {
//        mItems.clear();
//    }
//
//    public void addItem(YourObject yourObject) {
//        mItems.add(yourObject);
//    }
//
//    public void addItems(List<YourObject> yourObjectList) {
//        mItems.addAll(yourObjectList);
//    }
//
//    @Override
//    public int getCount() {
//        return mItems.size();
//    }
//
//    @Override
//    public Object getItem(int position) {
//        return mItems.get(position);
//    }
//
//    @Override
//    public long getItemId(int position) {
//        return position;
//    }
//
//    @Override
//    public View getDropDownView(int position, View view, ViewGroup parent) {
//        if (view == null || !view.getTag().toString().equals("DROPDOWN")) {
//            view = getLayoutInflater().inflate(R.layout.toolbar_spinner_item_dropdown, parent, false);
//            view.setTag("DROPDOWN");
//        }
//
//        TextView textView = (TextView) view.findViewById(android.R.id.text1);
//        textView.setText(getTitle(position));
//
//        return view;
//    }
//
//    @Override
//    public View getView(int position, View view, ViewGroup parent) {
//        if (view == null || !view.getTag().toString().equals("NON_DROPDOWN")) {
//            view = getLayoutInflater().inflate(R.layout.
//                    toolbar_spinner_item_actionbar, parent, false);
//            view.setTag("NON_DROPDOWN");
//        }
//        TextView textView = (TextView) view.findViewById(android.R.id.text1);
//        textView.setText(getTitle(position));
//        return view;
//    }
//
//    private String getTitle(int position) {
//        return position >= 0 && position < mItems.size() ? mItems.get(position).title : "";
//    }
//}
