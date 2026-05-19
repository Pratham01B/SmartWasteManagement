import { createClient } from '@supabase/supabase-js'

const supabaseUrl = 'https://grrtfxieugqfwigskfwl.supabase.co'
const supabaseAnonKey = 'sb_publishable_Ajqv_yqphVNyvQRxfyDN-w_Z9iac4iD'  // ← Dashboard se copy karein

export const supabase = createClient(supabaseUrl, supabaseAnonKey)