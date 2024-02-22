import psycopg2
from psycopg2.extras import Json
import json

def main():
    try: 
        # NOTE: change dbname and user when copying it into the test_db server instance
        conn = psycopg2.connect(dbname="ogc_collections", user="user")
        cursor=conn.cursor(cursor_factory=psycopg2.extras.RealDictCursor)
        # async later? 
        print (f'''Connection to {conn} successful''')
        # here you can pass the string as column_names for different schema/collections
        table_name = '<table_name>'
        columns = ''
        cursor.execute(f"select {columns} from {table_name}")
        rows = cursor.fetchall()
        count = 1
        # column_names = ['fid','objectid', 'area', 'perimeter', 'indiadpt_', 'indiadpt_i', 'hq', 'town', 'state', 'taluk', 'district']
        # this needs to be optimised when the number of rows is >>> 1000
        for row in rows:
            # fid is the unique key/column
            fid = row["fid"]
            print (f'''fid- {fid}, count- {count}''')
            row_to_json = json.dumps(row)
            # NOTE: properties column should exist beforehand
            # alter table <tablename> add column properties jsonb default '{}'            
            sql_str = f'''update {table_name} set properties = %s::json where fid=%s'''
            cursor.execute(sql_str, (row_to_json,fid))
            count+=1
        # the same column_names string/list can be passed here from the config    
        # for column_name in column_names:
        #    cursor.execute(f'''alter table district_hq drop column {column_name}''')
        conn.commit()
        conn.close()
    except Exception as e:
        print ("Something went wrong: ", e)
        print ("Exception type: ", type(e))

if __name__ == "__main__":
    main()
