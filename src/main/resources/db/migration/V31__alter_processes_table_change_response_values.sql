-- Update all rows in processes_table to replace 'VALUE' with 'value' and 'REFERENCE' with 'reference'
UPDATE public.processes_table
SET response = (
  SELECT array_agg(
    CASE elem
      WHEN 'VALUE' THEN 'value'::transmission_mode
      WHEN 'REFERENCE' THEN 'reference'::transmission_mode
      ELSE elem
    END
  )
  FROM unnest(response) AS elem
);
